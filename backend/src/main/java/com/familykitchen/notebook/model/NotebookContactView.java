package com.familykitchen.notebook.model;

/** Confirmed account contact, independent of family relationships.
 * @param userId owning account
 * @param contactUserId confirmed other account
 * @param source lookup source */
public record NotebookContactView(long userId, long contactUserId, String source) {}
