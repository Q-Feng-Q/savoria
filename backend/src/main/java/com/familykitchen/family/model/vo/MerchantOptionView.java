package com.familykitchen.family.model.vo;

/**
 * 封装返回给调用方的商户选项数据。
 *
 * @param id 标识
 * @param name 名称
 */
public record MerchantOptionView(Long id, String name) {}
