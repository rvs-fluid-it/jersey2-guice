/*
 * Copyright 2014 Squarespace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.squarespace.jersey2.guice;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.ext.RuntimeDelegate;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.internal.AbstractRuntimeDelegate;
import org.glassfish.jersey.internal.inject.Providers;
import org.glassfish.jersey.message.internal.JerseyLink;
import org.glassfish.jersey.message.internal.OutboundJaxrsResponse;
import org.glassfish.jersey.message.internal.OutboundMessageContext;
import org.glassfish.jersey.message.internal.VariantListBuilder;
import org.glassfish.jersey.server.ContainerFactory;
import org.glassfish.jersey.server.internal.RuntimeDelegateImpl;
import org.glassfish.jersey.spi.HeaderDelegateProvider;
import org.glassfish.jersey.uri.internal.JerseyUriBuilder;

import java.net.URI;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * This class replicates Jersey's own {@link RuntimeDelegateImpl} class with 
 * the difference that we're passing in a {@link ServiceLocator} instead of
 * creating a new one.
 * 
 * @see RuntimeDelegateImpl
 */
class GuiceRuntimeDelegate extends RuntimeDelegate {
  private final Set<HeaderDelegateProvider> hps;
  private final Map<Class<?>, HeaderDelegate<?>> map;

  public GuiceRuntimeDelegate(ServiceLocator serviceLocator) {
    hps = Providers.getProviders(serviceLocator, HeaderDelegateProvider.class);

    /**
     * Construct a map for quick look up of known header classes
     */
    map = new WeakHashMap<Class<?>, HeaderDelegate<?>>();
    map.put(EntityTag.class, _createHeaderDelegate(EntityTag.class));
    map.put(MediaType.class, _createHeaderDelegate(MediaType.class));
    map.put(CacheControl.class, _createHeaderDelegate(CacheControl.class));
    map.put(NewCookie.class, _createHeaderDelegate(NewCookie.class));
    map.put(Cookie.class, _createHeaderDelegate(Cookie.class));
    map.put(URI.class, _createHeaderDelegate(URI.class));
    map.put(Date.class, _createHeaderDelegate(Date.class));
    map.put(String.class, _createHeaderDelegate(String.class));
  }
  
  /**
   * @see RuntimeDelegateImpl#createEndpoint(Application, Class)
   */
  @Override
  public <T> T createEndpoint(Application application, Class<T> endpointType) 
      throws IllegalArgumentException, UnsupportedOperationException {
    
    if (application == null) {
      throw new IllegalArgumentException("application is null.");
    }
    
    return ContainerFactory.createContainer(endpointType, application);
  }

  @Override
  public javax.ws.rs.core.Variant.VariantListBuilder createVariantListBuilder() {
    return new VariantListBuilder();
  }

  @Override
  public Response.ResponseBuilder createResponseBuilder() {
    return new OutboundJaxrsResponse.Builder(new OutboundMessageContext());
  }

  @Override
  public UriBuilder createUriBuilder() {
    return new JerseyUriBuilder();
  }

  @Override
  public Link.Builder createLinkBuilder() {
    return new JerseyLink.Builder();
  }

  @Override
  public <T> HeaderDelegate<T> createHeaderDelegate(final Class<T> type) {
    if (type == null) {
      throw new IllegalArgumentException("type parameter cannot be null");
    }

    @SuppressWarnings("unchecked") final HeaderDelegate<T> delegate = (HeaderDelegate<T>) map.get(type);
    if (delegate != null) {
      return delegate;
    }

    return _createHeaderDelegate(type);
  }

  @SuppressWarnings("unchecked")
  private <T> HeaderDelegate<T> _createHeaderDelegate(final Class<T> type) {
    for (final HeaderDelegateProvider hp : hps) {
      if (hp.supports(type)) {
        return hp;
      }
    }

    return null;
  }

}
