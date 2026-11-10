package com.smartcommunity.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcommunity.common.result.Result;
import com.smartcommunity.entity.LifeOrder;
import com.smartcommunity.entity.LifeOrderItem;
import com.smartcommunity.entity.LifeProduct;
import com.smartcommunity.payment.PaymentResult;
import com.smartcommunity.service.LifeOrderItemService;
import com.smartcommunity.service.LifeOrderPaymentService;
import com.smartcommunity.service.LifeOrderService;
import com.smartcommunity.service.LifeProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Tag(name = "生活服务-订单")
@RestController
@RequestMapping("/api/v1/order")
@RequiredArgsConstructor
public class LifeOrderController {

    private final LifeOrderService orderService;
    private final LifeOrderItemService orderItemService;
    private final LifeProductService productService;
    private final LifeOrderPaymentService lifeOrderPaymentService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping("/list")
    public Result<Page<LifeOrder>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<LifeOrder> qw = new LambdaQueryWrapper<>();
        if (orderNo != null) qw.like(LifeOrder::getOrderNo, orderNo);
        if (status != null) qw.eq(LifeOrder::getStatus, status);
        qw.orderByDesc(LifeOrder::getId);
        Page<LifeOrder> result = orderService.page(new Page<>(page, pageSize), qw);
        for (LifeOrder order : result.getRecords()) {
            fillDisplay(order);
        }
        return Result.success(result);
    }

    @GetMapping("/{id}")
    public Result<LifeOrder> getById(@PathVariable Long id) {
        LifeOrder order = orderService.getById(id);
        if (order != null) {
            fillDisplay(order);
        }
        return Result.success(order);
    }

    @PostMapping
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> create(@RequestBody LifeOrder entity) {
        if (entity.getOrderNo() == null || entity.getOrderNo().isBlank()) {
            String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            int rnd = ThreadLocalRandom.current().nextInt(1000, 9999);
            entity.setOrderNo("ORD" + ts + rnd);
        }
        if (entity.getStatus() == null) {
            entity.setStatus(0);
        }
        if (entity.getOrderType() == null) {
            entity.setOrderType(1);
        }
        // 兼容前端字段映射
        if (entity.getReceiverName() == null || entity.getReceiverName().isBlank()) {
            entity.setReceiverName(entity.getUserName() != null ? entity.getUserName() : ("用户" + entity.getUserId()));
        }
        if (entity.getReceiverPhone() == null || entity.getReceiverPhone().isBlank()) {
            entity.setReceiverPhone(entity.getPhone());
        }
        if (entity.getReceiverAddress() == null || entity.getReceiverAddress().isBlank()) {
            entity.setReceiverAddress(entity.getAddress());
        }

        List<Map<String, Object>> itemMaps = parseItems(entity.getItems());
        if (itemMaps.isEmpty() && entity.getItemList() != null) {
            for (LifeOrderItem it : entity.getItemList()) {
                if (it == null || it.getProductId() == null) continue;
                Map<String, Object> m = new java.util.HashMap<>();
                m.put("productId", it.getProductId());
                m.put("productName", it.getProductName());
                m.put("price", it.getPrice());
                m.put("quantity", it.getQuantity() == null ? 1 : it.getQuantity());
                itemMaps.add(m);
            }
        }
        if (itemMaps.isEmpty()) {
            return Result.fail("订单商品明细不能为空");
        }

        // 计算总金额
        BigDecimal total = entity.getTotalAmount() == null ? BigDecimal.ZERO : entity.getTotalAmount();
        if ((total.compareTo(BigDecimal.ZERO) <= 0) && !itemMaps.isEmpty()) {
            total = BigDecimal.ZERO;
            for (Map<String, Object> m : itemMaps) {
                BigDecimal price = toDecimal(m.get("price"));
                int qty = toInt(m.get("quantity"), 1);
                total = total.add(price.multiply(BigDecimal.valueOf(qty)));
            }
            entity.setTotalAmount(total);
        }
        if (entity.getPayableAmount() == null) {
            entity.setPayableAmount(entity.getTotalAmount() == null ? BigDecimal.ZERO : entity.getTotalAmount());
        }
        if (entity.getDiscountAmount() == null) entity.setDiscountAmount(BigDecimal.ZERO);
        if (entity.getFreightAmount() == null) entity.setFreightAmount(BigDecimal.ZERO);
        if (entity.getTotalAmount() == null) entity.setTotalAmount(entity.getPayableAmount());

        Result<Void> stockResult = adjustStockFromMaps(itemMaps, true);
        if (!stockResult.isSuccess()) {
            return stockResult;
        }

        orderService.save(entity);
        saveOrderItems(entity.getId(), itemMaps);
        return Result.success();
    }

    @PutMapping
    public Result<Void> update(@RequestBody LifeOrder entity) {
        // 计算总金额
        if (entity.getPhone() != null) entity.setReceiverPhone(entity.getPhone());
        if (entity.getAddress() != null) entity.setReceiverAddress(entity.getAddress());
        if (entity.getUserName() != null) entity.setReceiverName(entity.getUserName());
        orderService.updateById(entity);
        return Result.success();
    }

    @Operation(summary = "支付订单（经统一支付网关；默认 mock 通道，非真实资金）")
    @PutMapping("/{id}/pay")
    public Result<Map<String, Object>> pay(@PathVariable Long id,
                                           @RequestBody(required = false) Map<String, Object> body) {
        Integer payMethod = null;
        if (body != null && body.get("payMethod") != null && !body.get("payMethod").toString().isBlank()) {
            payMethod = Integer.valueOf(body.get("payMethod").toString());
        }
        PaymentResult result = lifeOrderPaymentService.pay(id, payMethod);
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("success", result.isSuccess());
        data.put("simulated", result.isSimulated());
        data.put("channel", result.getChannel() != null ? result.getChannel().name() : null);
        data.put("tradeNo", result.getTradeNo());
        data.put("amount", result.getAmount());
        data.put("payMethod", result.getPayMethod());
        data.put("paidAt", result.getPaidAt());
        data.put("message", result.getMessage());
        String msg = result.isSimulated() ? "模拟支付成功（非真实资金）" : "支付成功";
        return Result.success(msg, data);
    }

    @Operation(summary = "发货")
    @PutMapping("/{id}/ship")
    public Result<Void> ship(@PathVariable Long id) {
        LifeOrder order = orderService.getById(id);
        if (order == null) return Result.fail("订单不存在");
        if (order.getStatus() == null || order.getStatus() != 1) {
            return Result.fail("当前状态不可操作");
        }
        order.setStatus(2);
        order.setDeliveryTime(LocalDateTime.now());
        orderService.updateById(order);
        return Result.success();
    }

    @Operation(summary = "确认收货/完成")
    @PutMapping("/{id}/complete")
    public Result<Void> complete(@PathVariable Long id) {
        LifeOrder order = orderService.getById(id);
        if (order == null) return Result.fail("订单不存在");
        if (order.getStatus() == null || order.getStatus() != 2) {
            return Result.fail("当前状态不可操作");
        }
        order.setStatus(3);
        order.setReceiveTime(LocalDateTime.now());
        orderService.updateById(order);
        return Result.success();
    }

    @PutMapping("/{id}/cancel")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> cancel(@PathVariable Long id) {
        LifeOrder order = orderService.getById(id);
        if (order == null) return Result.fail("订单不存在");
        if (order.getStatus() != null && order.getStatus() >= 3) {
            return Result.fail("已完成/已取消订单不可取消");
        }
        List<LifeOrderItem> items = orderItemService.list(new LambdaQueryWrapper<LifeOrderItem>()
                .eq(LifeOrderItem::getOrderId, id));
        List<Map<String, Object>> maps = new ArrayList<>();
        for (LifeOrderItem it : items) {
            maps.add(Map.of(
                    "productId", it.getProductId() == null ? 0 : it.getProductId(),
                    "quantity", it.getQuantity() == null ? 1 : it.getQuantity()
            ));
        }
        if (!maps.isEmpty() && order.getStatus() != null && order.getStatus() < 4) {
            adjustStockFromMaps(maps, false);
        }
        order.setStatus(4);
        order.setCancelTime(LocalDateTime.now());
        orderService.updateById(order);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        orderItemService.remove(new LambdaQueryWrapper<LifeOrderItem>().eq(LifeOrderItem::getOrderId, id));
        orderService.removeById(id);
        return Result.success();
    }

    private void fillDisplay(LifeOrder order) {
        order.setUserName(order.getReceiverName());
        order.setPhone(order.getReceiverPhone());
        order.setAddress(order.getReceiverAddress());
        List<LifeOrderItem> items = orderItemService.list(new LambdaQueryWrapper<LifeOrderItem>()
                .eq(LifeOrderItem::getOrderId, order.getId()));
        order.setItemList(items);
        if (!items.isEmpty()) {
            String summary = items.stream()
                    .map(i -> (i.getProductName() == null ? ("商品" + i.getProductId()) : i.getProductName())
                            + "×" + (i.getQuantity() == null ? 1 : i.getQuantity()))
                    .collect(Collectors.joining("，"));
            order.setItems(summary);
        }
    }

    private void saveOrderItems(Long orderId, List<Map<String, Object>> itemMaps) {
        for (Map<String, Object> m : itemMaps) {
            if (m.get("productId") == null) continue;
            Long productId = Long.valueOf(m.get("productId").toString());
            int qty = toInt(m.get("quantity"), 1);
            LifeProduct product = productService.getById(productId);
            BigDecimal price = toDecimal(m.get("price"));
            String name = m.get("productName") != null ? String.valueOf(m.get("productName")) : null;
            if (product != null) {
                if (price.compareTo(BigDecimal.ZERO) == 0 && product.getPrice() != null) {
                    price = product.getPrice();
                }
                if (name == null || name.isBlank()) {
                    name = product.getProductName();
                }
            }
            LifeOrderItem item = new LifeOrderItem();
            item.setOrderId(orderId);
            item.setProductId(productId);
            item.setProductName(name == null ? ("商品" + productId) : name);
            item.setPrice(price);
            item.setQuantity(qty);
            item.setTotalPrice(price.multiply(BigDecimal.valueOf(qty)));
            item.setCreateTime(LocalDateTime.now());
            orderItemService.save(item);
        }
    }

    private List<Map<String, Object>> parseItems(String itemsJson) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (itemsJson == null || itemsJson.isBlank()) {
            return list;
        }
        try {
            list = objectMapper.readValue(itemsJson, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception ignored) {
        }
        return list == null ? new ArrayList<>() : list;
    }

    private Result<Void> adjustStockFromMaps(List<Map<String, Object>> items, boolean deduct) {
        if (items == null || items.isEmpty()) {
            return Result.success();
        }
        try {
            for (Map<String, Object> item : items) {
                if (item.get("productId") == null) continue;
                Long productId = Long.valueOf(item.get("productId").toString());
                int qty = toInt(item.get("quantity"), 1);
                if (qty <= 0) continue;
                LifeProduct product = productService.getById(productId);
                if (product == null) {
                    return Result.fail("商品不存在: " + productId);
                }
                int stock = product.getStock() == null ? 0 : product.getStock();
                int sales = product.getSales() == null ? 0 : product.getSales();
                if (deduct) {
                    if (stock < qty) {
                        return Result.fail("库存不足: " + product.getProductName());
                    }
                    product.setStock(stock - qty);
                    product.setSales(sales + qty);
                } else {
                    product.setStock(stock + qty);
                    product.setSales(Math.max(0, sales - qty));
                }
                productService.updateById(product);
            }
        } catch (Exception e) {
            return Result.fail("库存调整失败: " + e.getMessage());
        }
        return Result.success();
    }

    private BigDecimal toDecimal(Object v) {
        if (v == null) return BigDecimal.ZERO;
        try {
            return new BigDecimal(String.valueOf(v));
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private int toInt(Object v, int def) {
        if (v == null) return def;
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (Exception e) {
            return def;
        }
    }
}
