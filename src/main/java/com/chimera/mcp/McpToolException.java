package com.chimera.mcp;

/**
 * Exception that represents a failure in calling an MCP tool.
 */
public class McpToolException extends RuntimeException {

  public McpToolException(String message) {
    super(message);
  }

  public McpToolException(String message, Throwable cause) {
    super(message, cause);
  }
}
