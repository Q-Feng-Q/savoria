package com.familykitchen.system.model.dto;

import com.familykitchen.system.validation.BrandUrl;
import com.familykitchen.system.validation.BrandSizeDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.*;

/** Null means unchanged; an empty URL removes that override. */
public record BrandSettingRequest(
    @Size(max=100) @Pattern(regexp="(?s).*\\S.*") String siteName,
    @BrandUrl @Size(max=500) String siteLogoUrl,
    @BrandUrl @Size(max=500) String siteLogoSmallUrl,
    @BrandUrl @Size(max=500) String siteLogoLargeUrl,
    @BrandUrl @Size(max=500) String siteFaviconUrl,
    @JsonDeserialize(using=BrandSizeDeserializer.class) @Min(16) @Max(64) Integer siteLogoSmallSize,
    @JsonDeserialize(using=BrandSizeDeserializer.class) @Min(24) @Max(120) Integer siteLogoSize,
    @JsonDeserialize(using=BrandSizeDeserializer.class) @Min(48) @Max(160) Integer siteLogoLargeSize
) {}
