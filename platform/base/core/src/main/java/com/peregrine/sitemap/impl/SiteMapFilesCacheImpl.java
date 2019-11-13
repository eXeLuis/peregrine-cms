package com.peregrine.sitemap.impl;

/*-
 * #%L
 * platform base - Core
 * %%
 * Copyright (C) 2017 headwire inc.
 * %%
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * #L%
 */

import com.peregrine.sitemap.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.*;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import java.util.*;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Component(service = SiteMapFilesCache.class)
@Designate(ocd = SiteMapFilesCacheImplConfig.class)
public final class SiteMapFilesCacheImpl extends CacheBuilderBase implements SiteMapFilesCache {

    private static final String MAIN_SITE_MAP_KEY = Integer.toString(0);

    @Reference
    private ResourceResolverFactoryProxy resourceResolverFactory;

    @Reference
    private SiteMapStructureCache structureCache;

    @Reference
    private SiteMapExtractorsContainer siteMapExtractorsContainer;

    @Reference
    private SiteMapFileContentBuilder siteMapBuilder;

    private int maxEntriesCount;
    private int maxFileSize;

    @Activate
    public void activate(final SiteMapFilesCacheImplConfig config) {
        setLocation(config.location());

        maxEntriesCount = config.maxEntriesCount();
        if (maxEntriesCount <= 0) {
            maxEntriesCount = Integer.MAX_VALUE;
        }

        maxFileSize = config.maxFileSize();
        if (maxFileSize <= 0) {
            maxFileSize = Integer.MAX_VALUE;
        }

        rebuildAll();
    }

    @Override
    public String get(final Resource rootPage, final int index) {
        try (final ResourceResolver resourceResolver = getServiceResourceResolver()) {
            final Resource cache;
            if (isCached(resourceResolver, rootPage.getPath())) {
                final String cachePath = getCachePath(rootPage);
                cache = resourceResolver.getResource(cachePath);
            } else {
                cache = buildCache(resourceResolver, rootPage);
                resourceResolver.commit();
            }

            final ValueMap properties = cache.getValueMap();
            final String key = Integer.toString(index);
            return properties.get(key, String.class);
        } catch (final LoginException e) {
            logger.error(COULD_NOT_GET_SERVICE_RESOURCE_RESOLVER, e);
            return null;
        } catch (final PersistenceException e) {
            logger.error(COULD_NOT_SAVE_SITE_MAP_CACHE, e);
            return null;
        }
    }

    protected ResourceResolver getServiceResourceResolver() throws LoginException {
        return resourceResolverFactory.getServiceResourceResolver();
    }

    protected boolean containsCacheAlready(final Resource cache) {
        return Optional.ofNullable(cache)
                .map(Resource::getValueMap)
                .map(vm -> vm.containsKey(MAIN_SITE_MAP_KEY))
                .orElse(false);
    }

    protected String getCachePath(final String rootPagePath) {
        return location + rootPagePath;
    }

    protected String getOriginalPath(final String cachePath) {
        if (!StringUtils.startsWith(cachePath, locationWithSlash)) {
            return null;
        }

        return StringUtils.substringAfter(cachePath, location);
    }

    protected Resource buildCache(final Resource rootPage, final Resource cache) {
        final Collection<SiteMapEntry> entries = structureCache.get(rootPage);
        if (isNull(entries)) {
            putSiteMapsInCache(null, cache);
            return null;
        }

        final ArrayList<String> siteMaps = new ArrayList<>();
        final LinkedList<List<SiteMapEntry>> splitEntries = splitEntries(entries);
        final int numberOfParts = splitEntries.size();
        if (numberOfParts > 1) {
            final SiteMapExtractor extractor = siteMapExtractorsContainer.findFirstFor(rootPage);
            siteMaps.add(siteMapBuilder.buildSiteMapIndex(rootPage, extractor, numberOfParts));
        }

        for (final List<SiteMapEntry> list : splitEntries) {
            siteMaps.add(siteMapBuilder.buildUrlSet(list));
        }

        putSiteMapsInCache(siteMaps, cache);

        return cache;
    }

    private LinkedList<List<SiteMapEntry>> splitEntries(final Collection<SiteMapEntry> entries) {
        final int baseSiteMapLength = siteMapBuilder.getBaseSiteMapLength();
        final LinkedList<List<SiteMapEntry>> result = new LinkedList<>();
        int index = 0;
        int size = baseSiteMapLength;
        List<SiteMapEntry> split = new LinkedList<>();
        result.add(split);
        for (final SiteMapEntry entry : entries) {
            final int entrySize = siteMapBuilder.getSize(entry);
            if (index < maxEntriesCount && size + entrySize <= maxFileSize) {
                index++;
                size += entrySize;
            } else {
                index = 1;
                size = baseSiteMapLength;
                result.add(split = new LinkedList<>());
            }

            split.add(entry);
        }

        return result;
    }

    private void putSiteMapsInCache(final ArrayList<String> source, final Resource target) {
        final ModifiableValueMap modifiableValueMap = target.adaptTo(ModifiableValueMap.class);
        final int siteMapsSize = nonNull(source) ? source.size() : 0;
        for (int i = 0; i < siteMapsSize; i++) {
            modifiableValueMap.put(Integer.toString(i), source.get(i));
        }

        removeCachedItemsAboveIndex(modifiableValueMap, siteMapsSize);
    }

    private void removeCachedItemsAboveIndex(final ModifiableValueMap modifiableValueMap, final int startItemIndex) {
        int i = startItemIndex;
        String key = Integer.toString(i);
        while (modifiableValueMap.containsKey(key)) {
            modifiableValueMap.remove(key);
            key = Integer.toString(++i);
        }
    }

    @Override
    protected void rebuildImpl(final String rootPagePath) {
        buildCache(rootPagePath);
    }
}
