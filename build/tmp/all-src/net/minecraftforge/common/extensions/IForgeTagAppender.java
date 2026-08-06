/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.common.extensions;

import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

public interface IForgeTagAppender<T>
{
    private TagsProvider.TagAppender<T> self() {
        return (TagsProvider.TagAppender<T>) this;
    }

    @SuppressWarnings("unchecked")
    default TagsProvider.TagAppender<T> addTags(TagKey<T>... values) {
        TagsProvider.TagAppender<T> builder = self();
        for (TagKey<T> value : values) {
            builder.m_206428_(value);
        }
        return builder;
    }

    default TagsProvider.TagAppender<T> addOptionalTag(TagKey<T> value) {
        return self().m_176841_(value.f_203868_());
    }

    @SuppressWarnings("unchecked")
    default TagsProvider.TagAppender<T> addOptionalTags(TagKey<T>... values) {
        TagsProvider.TagAppender<T> builder = self();
        for (TagKey<T> value : values) {
            builder.m_176841_(value.f_203868_());
        }
        return builder;
    }

    default TagsProvider.TagAppender<T> replace() {
        return replace(true);
    }

    default TagsProvider.TagAppender<T> replace(boolean value) {
        self().getInternalBuilder().replace(value);
        return self();
    }

    /**
     * Adds a single element's ID to the tag json's remove list. Callable during datageneration.
     * @param location The ID of the element to remove
     * @return The builder for chaining
     */
    default TagsProvider.TagAppender<T> remove(final ResourceLocation location)
    {
        TagsProvider.TagAppender<T> builder = self();
        builder.getInternalBuilder().removeElement(location, builder.getModID());
        return builder;
    }

    /**
     * Adds multiple elements' IDs to the tag json's remove list. Callable during datageneration.
     * @param locations The IDs of the elements to remove
     * @return The builder for chaining
     */
    default TagsProvider.TagAppender<T> remove(final ResourceLocation first, final ResourceLocation... locations)
    {
        this.remove(first);
        for (ResourceLocation location : locations)
        {
            this.remove(location);
        }
        return self();
    }

    /**
     * Adds a resource key to the tag json's remove list. Callable during datageneration.
     *
     * @param resourceKey The resource key of the element to remove
     * @return The appender for chaining
     */
    default TagsProvider.TagAppender<T> remove(final ResourceKey<T> resourceKey)
    {
        this.remove(resourceKey.m_135782_());
        return self();
    }

    /**
     * Adds multiple resource keys to the tag json's remove list. Callable during datageneration.
     *
     * @param resourceKeys The resource keys of the elements to remove
     * @return The appender for chaining
     */
    @SuppressWarnings("unchecked")
    default TagsProvider.TagAppender<T> remove(final ResourceKey<T> firstResourceKey, final ResourceKey<T>... resourceKeys)
    {
        this.remove(firstResourceKey.m_135782_());
        for (ResourceKey<T> resourceKey : resourceKeys)
        {
            this.remove(resourceKey.m_135782_());
        }
        return self();
    }

    /**
     * Adds a tag to the tag json's remove list. Callable during datageneration.
     * @param tag The ID of the tag to remove
     * @return The builder for chaining
     */
    default TagsProvider.TagAppender<T> remove(TagKey<T> tag)
    {
        TagsProvider.TagAppender<T> builder = self();
        builder.getInternalBuilder().removeTag(tag.f_203868_(), builder.getModID());
        return builder;
    }

    /**
     * Adds multiple tags to the tag json's remove list. Callable during datageneration.
     * @param tags The IDs of the tags to remove
     * @return The builder for chaining
     */
    @SuppressWarnings("unchecked")
    default TagsProvider.TagAppender<T> remove(TagKey<T> first, TagKey<T>...tags)
    {
        this.remove(first);
        for (TagKey<T> tag : tags)
        {
            this.remove(tag);
        }
        return self();
    }
}
