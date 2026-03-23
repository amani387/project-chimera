package com.chimera.model;

public record ImageRequest(
    String prompt,
    String style,
    String negativePrompt,
    Integer width,
    Integer height
) {
  public ImageRequest {
    prompt = prompt != null ? prompt : "";
    style = style != null ? style : "";
    negativePrompt = negativePrompt != null ? negativePrompt : "";
    width = width != null ? width : 1024;
    height = height != null ? height : 1024;
  }
}
