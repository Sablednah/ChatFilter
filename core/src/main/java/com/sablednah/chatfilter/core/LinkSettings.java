package com.sablednah.chatfilter.core;

import java.util.List;

/**
 * @param ips            also catch bare IP addresses ({@code 192.168.1.20:25565})
 * @param spelledDots    also catch {@code example dot com}, {@code example(.)com} and friends
 * @param tlds           top-level domains that make {@code word.tld} a link
 * @param allowedDomains domains that may always be posted — and their subdomains
 */
public final class LinkSettings {

    private final CategoryPolicy policy;
    private final boolean ips;
    private final boolean spelledDots;
    private final List<String> tlds;
    private final List<String> allowedDomains;

    public LinkSettings(CategoryPolicy policy, boolean ips, boolean spelledDots, List<String> tlds, List<String> allowedDomains) {
        this.policy = policy;
        this.ips = ips;
        this.spelledDots = spelledDots;
        this.tlds = tlds;
        this.allowedDomains = allowedDomains;
    }

    public CategoryPolicy policy() {
        return policy;
    }

    public boolean ips() {
        return ips;
    }

    public boolean spelledDots() {
        return spelledDots;
    }

    public List<String> tlds() {
        return tlds;
    }

    public List<String> allowedDomains() {
        return allowedDomains;
    }

}
