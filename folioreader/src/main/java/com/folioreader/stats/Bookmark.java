package com.folioreader.stats;

/** A saved reading position. {@code locator} is the ReadLocator JSON used to reopen the book there. */
public class Bookmark {
    public long id;
    public String bookKey;
    public String bookTitle;
    public String chapter;
    public float progress;
    public String locator;
    public String note;
    public long created;
}
