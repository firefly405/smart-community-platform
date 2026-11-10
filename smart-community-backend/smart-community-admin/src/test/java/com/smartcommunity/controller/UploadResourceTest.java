package com.smartcommunity.controller;

import com.smartcommunity.config.WebMvcConfig;
import com.smartcommunity.security.AuthInterceptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UploadResourceTest {
    @TempDir Path temporary;
    @Test void missingUploadDirectoryStillRegistersDirectoryLocation() {
        WebMvcConfig config = new WebMvcConfig(mock(AuthInterceptor.class));
        Path missing = temporary.resolve("not-created");
        ReflectionTestUtils.setField(config, "uploadDir", missing.toString());
        ResourceHandlerRegistry registry = new ResourceHandlerRegistry(new StaticWebApplicationContext(), new MockServletContext());
        config.addResourceHandlers(registry);
        List<?> registrations = (List<?>) ReflectionTestUtils.getField(registry, "registrations");
        List<?> locations = (List<?>) ReflectionTestUtils.getField(registrations.get(0), "locationValues");
        assertEquals(missing.toUri().toString() + "/", locations.get(0));
    }
}
