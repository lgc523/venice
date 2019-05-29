package com.linkedin.venice.acl;

import java.security.cert.X509Certificate;


/**
 * An AccessController allows a request to be checked against an Access Control List (ACL).
 */
public interface AccessController {

  /**
   * Check if client has permission to access a particular resource.
   * This method is invoked by every single request, therefore
   * minimized execution time will result the best latency and throughput.
   *
   * @param clientCert the X509Certificate submitted by client
   * @param resource the resource being requested
   * @param method the operation (GET, POST, ...) to perform against the resource
   * @return {@code true} if client has permission to access, otherwise {@code false}.
   */
  boolean hasAccess(X509Certificate clientCert, String resource, String method) throws AclException;
}