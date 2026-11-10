# 二手演示物品配图

15张本地JPEG，统一为960×720，采用等比缩放和留白，不裁切物品主体。来自Wikimedia Commons；不是卖家实拍，也不保证型号、配件及成色与交易物品一致。

逐图来源、作者和许可见前端 `src/assets/marketplace-image-credits.json`，用户端详情展示相应署名和许可链接。CC BY-SA图片的此处缩放版本继续遵循原始同版本许可；其他图片遵循清单所列许可。再分发本目录时必须保留清单及署名。

准备图片：在项目根目录运行 `./smart-community-backend/scripts/Prepare-MarketplaceImages.ps1`。脚本下载并生成本目录素材，复制到默认运行时 `smart-community-admin/uploads/community`，生成署名清单及SQL，不直接修改数据库。Windows PowerShell 7及System.Drawing可用时运行；如配置了自定义UPLOAD_DIR，需将JPEG复制到该目录的community子目录。

导入：将 `smart-community-backend/sql/patch_secondhand_images.sql` 应用于项目数据库，只更新15个指定物品编号的空图片字段，不覆盖用户已有图片、不更改其他业务字段。

恢复：本目录 `rollback.sql` 只清除仍完全等于本次配图的图片字段，不影响之后替换的实拍图。本机导入前快照保存在工作目录的 `work/marketplace-images-before.json`；本次15条记录原图片均为空。
