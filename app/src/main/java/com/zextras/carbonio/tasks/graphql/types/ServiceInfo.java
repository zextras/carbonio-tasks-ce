// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.tasks.graphql.types;

/** GraphQL output type for service metadata. */
public class ServiceInfo {

  private final String name;
  private final String version;
  private final String flavour;

  public ServiceInfo(String name, String version, String flavour) {
    this.name = name;
    this.version = version;
    this.flavour = flavour;
  }

  public String getName() {
    return name;
  }

  public String getVersion() {
    return version;
  }

  public String getFlavour() {
    return flavour;
  }
}
