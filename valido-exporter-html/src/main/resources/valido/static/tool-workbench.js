(function () {
  var BASE64_ALGORITHM = "validohub.base64";
  var BASE64_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=_-";
  var textEncoder = new TextEncoder();
  var textDecoder = new TextDecoder("utf-8", { fatal: true });

  function bytesToBase64(bytes) {
    var binary = "";
    var chunkSize = 0x8000;
    for (var i = 0; i < bytes.length; i += chunkSize) {
      var chunk = bytes.subarray(i, i + chunkSize);
      binary += String.fromCharCode.apply(null, chunk);
    }
    return window.btoa(binary);
  }

  function encodeBytes(bytes, options) {
    var result = bytesToBase64(bytes);
    if (options.urlSafe) {
      result = result.replace(/\+/g, "-").replace(/\//g, "_");
    }
    if (!options.padding) {
      result = result.replace(/=+$/g, "");
    }
    return result;
  }

  function normalizeBase64(value) {
    var compact = value.replace(/\s+/g, "");
    var normalized = compact.replace(/-/g, "+").replace(/_/g, "/");
    var remainder = normalized.length % 4;
    if (remainder === 1) {
      throw new Error("Invalid length. Base64 length cannot leave a remainder of 1.");
    }
    if (remainder > 0) {
      normalized += "=".repeat(4 - remainder);
    }
    return {
      compact: compact,
      normalized: normalized
    };
  }

  function base64ToBytes(value) {
    var normalized = normalizeBase64(value);
    if (!/^[A-Za-z0-9+/]*={0,2}$/.test(normalized.normalized) || /=[^=]/.test(normalized.normalized)) {
      throw new Error("Unexpected padding or invalid Base64 alphabet.");
    }
    var binary = window.atob(normalized.normalized);
    var bytes = new Uint8Array(binary.length);
    for (var i = 0; i < binary.length; i++) {
      bytes[i] = binary.charCodeAt(i);
    }
    return {
      bytes: bytes,
      compact: normalized.compact,
      normalized: normalized.normalized
    };
  }

  function decodedText(bytes) {
    return textDecoder.decode(bytes);
  }

  function analyzeBase64(value) {
    var raw = value || "";
    var compact = raw.replace(/\s+/g, "");
    var details = [];
    var warnings = [];
    var diagnostics = [];
    var ignoredWhitespace = raw.length - compact.length;

    if (!compact) {
      return invalid("Enter Base64 text.", details, diagnostics, warnings);
    }

    var invalidCharacter = firstInvalidCharacter(raw);
    if (invalidCharacter) {
      diagnostics.push("Invalid character '" + invalidCharacter.character + "' at position " + invalidCharacter.position + ".");
      return invalid("Invalid character at position " + invalidCharacter.position + ".", details, diagnostics, warnings);
    }

    var hasUrlSafe = /[-_]/.test(compact);
    var hasStandardSpecials = /[+/]/.test(compact);
    if (hasUrlSafe && hasStandardSpecials) {
      warnings.push("Mixed alphabet warning: standard and URL-safe characters are both present.");
    }
    if (ignoredWhitespace > 0) {
      warnings.push("Whitespace note: " + ignoredWhitespace + " whitespace characters were ignored.");
    }

    var paddingIndex = compact.indexOf("=");
    var paddingCount = (compact.match(/=/g) || []).length;
    if (paddingIndex !== -1 && !/^=+$/.test(compact.slice(paddingIndex))) {
      diagnostics.push("Unexpected padding at position " + (paddingIndex + 1) + ".");
      return invalid("Unexpected padding. Padding must appear only at the end.", details, diagnostics, warnings);
    }
    if (paddingCount > 2) {
      diagnostics.push("Unexpected padding. Base64 can use at most two = characters.");
      return invalid("Unexpected padding. Too many padding characters.", details, diagnostics, warnings);
    }
    if (paddingCount > 0 && compact.length % 4 !== 0) {
      diagnostics.push("Invalid length. Padded Base64 length must be a multiple of 4.");
      return invalid("Invalid length for padded Base64.", details, diagnostics, warnings);
    }
    if (compact.length % 4 === 1) {
      diagnostics.push("Invalid length. Unpadded Base64 cannot have length modulo 4 equal to 1.");
      return invalid("Invalid length for Base64.", details, diagnostics, warnings);
    }

    var decoded;
    try {
      decoded = base64ToBytes(compact);
    } catch (error) {
      diagnostics.push(error.message);
      return invalid(error.message, details, diagnostics, warnings);
    }

    var text = "";
    var textStatus = "Binary or non-UTF-8";
    try {
      text = decodedText(decoded.bytes);
      textStatus = "UTF-8 text";
    } catch (error) {
      warnings.push("Decoded bytes are valid Base64 but are not valid UTF-8 text.");
    }

    var canonical = bytesToBase64(decoded.bytes);
    var canonicalUrl = canonical.replace(/\+/g, "-").replace(/\//g, "_");
    var isCanonical = compact === canonical
        || compact === canonical.replace(/=+$/g, "")
        || compact === canonicalUrl
        || compact === canonicalUrl.replace(/=+$/g, "");
    var variant = hasUrlSafe ? "Base64URL" : "Standard Base64";
    var padding = paddingState(compact, decoded.bytes.length, true);

    details.push(["Input characters", String(Array.from(raw).length)]);
    details.push(["Input UTF-8 bytes", formatBytes(textEncoder.encode(raw).length)]);
    details.push(["Output characters", textStatus === "UTF-8 text" ? String(Array.from(text).length) : "Binary output"]);
    details.push(["Decoded byte size", formatBytes(decoded.bytes.length)]);
    details.push(["Estimated decoded size", formatBytes(estimatedDecodedSize(compact))]);
    details.push(["Variant", variant]);
    details.push(["Padding", padding]);
    details.push(["Contains whitespace", ignoredWhitespace > 0 ? "Yes" : "No"]);
    details.push(["Canonical", isCanonical ? "Yes" : "No"]);
    details.push(["Decoded type", textStatus]);

    if (!isCanonical) {
      warnings.push("Input is valid but not canonical for its detected alphabet and padding style.");
    }

    return {
      valid: true,
      bytes: decoded.bytes,
      text: text,
      textStatus: textStatus,
      canonical: canonical,
      canonicalUrl: canonicalUrl,
      variant: variant,
      padding: padding,
      canonicalInput: isCanonical,
      hasWhitespace: ignoredWhitespace > 0,
      message: "Valid " + variant + ". Decoded size: " + formatBytes(decoded.bytes.length) + ".",
      details: details,
      diagnostics: diagnostics,
      warnings: warnings
    };
  }

  function invalid(message, details, diagnostics, warnings) {
    return {
      valid: false,
      message: message,
      details: details || [],
      diagnostics: diagnostics || [],
      warnings: warnings || []
    };
  }

  function firstInvalidCharacter(value) {
    for (var index = 0; index < value.length; index++) {
      var character = value.charAt(index);
      if (/\s/.test(character)) {
        continue;
      }
      if (BASE64_CHARS.indexOf(character) === -1) {
        return {
          character: character,
          position: index + 1
        };
      }
    }
    return null;
  }

  function paddingState(compact, decodedLength, valid) {
    if (!valid) {
      return "Invalid";
    }
    if (/=+$/.test(compact)) {
      return "Included";
    }
    if (decodedLength % 3 === 0) {
      return "Not required";
    }
    return "Omitted";
  }

  function estimatedDecodedSize(compact) {
    if (!compact) {
      return 0;
    }
    var padding = (compact.match(/=/g) || []).length;
    var normalizedLength = compact.length + ((4 - compact.length % 4) % 4);
    return Math.max(0, Math.floor(normalizedLength * 3 / 4) - padding);
  }

  function detectInputMode(value) {
    var compact = (value || "").replace(/\s+/g, "");
    if (!compact) {
      return { label: "Waiting for input", state: "" };
    }
    var invalidCharacter = firstInvalidCharacter(value);
    var hasBase64Signal = /[=+/_-]/.test(compact) || compact.length >= 8 || compact.length % 4 === 0;
    if (invalidCharacter) {
      return hasBase64Signal
          ? { label: "Invalid Base64", state: "invalid" }
          : { label: "Looks like text", state: "text" };
    }
    var analysis = analyzeBase64(value);
    if (!analysis.valid) {
      return hasBase64Signal
          ? { label: "Invalid Base64", state: "invalid" }
          : { label: "Looks like text", state: "text" };
    }
    if (/[-_]/.test(compact)) {
      return { label: "Looks like Base64URL", state: "base64url" };
    }
    if (hasBase64Signal) {
      return { label: "Looks like Base64", state: "base64" };
    }
    return { label: "Looks like text", state: "text" };
  }

  function formatBytes(count) {
    if (count === 1) {
      return "1 byte";
    }
    return count + " bytes";
  }

  function formatRatio(inputBytes, outputChars) {
    if (!inputBytes) {
      return "n/a";
    }
    return (outputChars / inputBytes).toFixed(2) + "x";
  }

  function formValues(form) {
    var values = {};
    form.querySelectorAll("textarea[name], input[name], select[name]").forEach(function (field) {
      if (field.type === "checkbox") {
        values[field.name] = field.checked;
      } else {
        values[field.name] = field.value;
      }
    });
    return values;
  }

  function primaryInput(form) {
    return form.querySelector("textarea[name=\"input\"]");
  }

  function setMessage(form, text, state) {
    var message = form.querySelector("[data-tool-message]");
    if (!message) {
      return;
    }
    message.textContent = text || "";
    message.dataset.state = state || "";
  }

  function setOutput(form, value) {
    var output = form.querySelector("[data-tool-output]");
    if (output) {
      output.value = value || "";
    }
  }

  function setBadge(form, mode) {
    var badge = form.querySelector("[data-input-mode-badge]");
    if (!badge) {
      return;
    }
    badge.textContent = mode.label;
    badge.dataset.state = mode.state || "";
  }

  function setFeedback(form, details, notes, state) {
    var feedback = form.querySelector("[data-tool-feedback]");
    if (!feedback) {
      return;
    }
    var rows = details || [];
    var messages = notes || [];
    if (rows.length === 0 && messages.length === 0) {
      feedback.innerHTML = "";
      feedback.dataset.state = "";
      return;
    }
    var html = "";
    if (rows.length > 0) {
      html += "<dl class=\"feedback-grid\">";
      rows.forEach(function (row) {
        html += "<div><dt>" + escapeHtml(row[0]) + "</dt><dd>" + escapeHtml(row[1]) + "</dd></div>";
      });
      html += "</dl>";
    }
    if (messages.length > 0) {
      html += "<ul class=\"feedback-notes\">";
      messages.forEach(function (note) {
        html += "<li>" + escapeHtml(note) + "</li>";
      });
      html += "</ul>";
    }
    feedback.innerHTML = html;
    feedback.dataset.state = state || "";
  }

  function setPreview(form, title, bodyHtml) {
    var preview = form.querySelector("[data-tool-preview]");
    if (!preview) {
      return;
    }
    preview.innerHTML = title ? "<div class=\"preview-title\">" + escapeHtml(title) + "</div><div class=\"preview-body\">" + bodyHtml + "</div>" : "";
  }

  function setAdvanced(form, html) {
    var advanced = form.querySelector("[data-advanced-panel]");
    var target = form.querySelector("[data-tool-advanced]");
    if (!advanced || !target) {
      return;
    }
    target.innerHTML = html || "";
    advanced.classList.toggle("has-content", Boolean(html));
  }

  function clearPanels(form) {
    setFeedback(form, [], [], "");
    setPreview(form, "", "");
    setAdvanced(form, "");
  }

  function escapeHtml(value) {
    return String(value)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;");
  }

  function runAction(form, action, options) {
    var algorithmId = form.dataset.algorithmId;
    var capability = form.dataset.capability;
    var values = formValues(form);
    var quiet = options && options.quiet;

    if (algorithmId === BASE64_ALGORITHM && capability === "encode" && action === "encode") {
      if (form._file) {
        encodeFile(form, values);
        return;
      }
      if (!values.input || !values.input.trim()) {
        setOutput(form, "");
        clearPanels(form);
        setMessage(form, quiet ? "" : "Enter text to encode.", quiet ? "" : "error");
        return;
      }
      var inputBytes = textEncoder.encode(values.input);
      var output = encodeBytes(inputBytes, {
        urlSafe: Boolean(values.urlSafe),
        padding: values.padding !== false
      });
      var details = encodeDetails(values.input, inputBytes, output, values);
      setOutput(form, output);
      setMessage(form, "Encoded live.", "success");
      setFeedback(form, details, [], "success");
      setPreview(form, "", "");
      setAdvanced(form, hexSection(inputBytes, "Input byte preview"));
      form._lastResult = {
        type: "encodedText",
        text: output,
        extension: "txt",
        mime: "text/plain;charset=utf-8"
      };
      return;
    }

    if (algorithmId === BASE64_ALGORITHM && capability === "encode" && action === "decode") {
      decodeIntoForm(form, values.input, quiet);
      return;
    }

    if (algorithmId === BASE64_ALGORITHM && capability === "validate" && action === "validate") {
      validateIntoForm(form, values.input, quiet);
      return;
    }

    setMessage(form, "Unable to run this action.", "error");
  }

  function encodeFile(form, values) {
    var file = form._file;
    var output = encodeBytes(file.bytes, {
      urlSafe: Boolean(values.urlSafe),
      padding: values.padding !== false
    });
    var details = [
      ["File name", file.name],
      ["File size", formatBytes(file.bytes.length)],
      ["MIME type", file.type || "Unknown"],
      ["Output characters", output.length + " characters"],
      ["Variant", values.urlSafe ? "Base64URL" : "Standard Base64"],
      ["Padding", values.padding !== false ? "Included" : "Omitted"],
      ["Expansion ratio", formatRatio(file.bytes.length, output.length)]
    ];
    setOutput(form, output);
    setMessage(form, "Encoded file locally.", "success");
    setFeedback(form, details, ["File bytes were read locally in this browser only."], "success");
    setPreview(form, "", "");
    setAdvanced(form, hexSection(file.bytes, "File byte preview"));
    form._lastResult = {
      type: "encodedFile",
      text: output,
      extension: "txt",
      mime: "text/plain;charset=utf-8",
      sourceName: file.name
    };
  }

  function encodeDetails(input, inputBytes, output, values) {
    return [
      ["Input characters", String(Array.from(input).length)],
      ["Input UTF-8 bytes", formatBytes(inputBytes.length)],
      ["Output characters", output.length + " characters"],
      ["Decoded byte size", formatBytes(inputBytes.length)],
      ["Estimated decoded size", formatBytes(inputBytes.length)],
      ["Variant", values.urlSafe ? "Base64URL" : "Standard Base64"],
      ["Padding", values.padding !== false ? "Included" : paddingState(output, inputBytes.length, true)],
      ["Expansion ratio", formatRatio(inputBytes.length, output.length)],
      ["Contains whitespace", /\s/.test(input) ? "Yes" : "No"],
      ["Canonical", "Yes"]
    ];
  }

  function decodeIntoForm(form, input, quiet) {
    if (!input || !input.trim()) {
      setOutput(form, "");
      clearPanels(form);
      setMessage(form, quiet ? "" : "Enter Base64 text to decode.", quiet ? "" : "error");
      return;
    }
    var decoded = analyzeBase64(input);
    if (!decoded.valid) {
      setOutput(form, "");
      setMessage(form, decoded.message, "error");
      setFeedback(form, decoded.details, decoded.diagnostics.concat(decoded.warnings), "error");
      setPreview(form, "", "");
      setAdvanced(form, "");
      return;
    }
    var preview = previewForBytes(decoded.bytes, decoded.text, decoded.textStatus);
    if (decoded.textStatus === "UTF-8 text") {
      setOutput(form, preview.outputText);
    } else {
      setOutput(form, "Decoded binary data. Use Download result to save " + formatBytes(decoded.bytes.length) + ".");
    }
    setMessage(form, "Decoded live.", decoded.warnings.length > 0 ? "warning" : "success");
    setFeedback(form, decoded.details, decoded.warnings, decoded.warnings.length > 0 ? "warning" : "success");
    setPreview(form, preview.title, preview.html);
    setAdvanced(form, hexSection(decoded.bytes, "Decoded byte preview"));
    form._lastResult = {
      type: preview.kind === "text" || preview.kind === "json" || preview.kind === "svg" ? "decodedText" : "decodedBinary",
      text: preview.outputText,
      bytes: decoded.bytes,
      extension: preview.extension,
      mime: preview.mime
    };
  }

  function validateIntoForm(form, input, quiet) {
    if (!input || !input.trim()) {
      setOutput(form, "");
      clearPanels(form);
      setMessage(form, quiet ? "" : "Enter Base64 text to validate.", quiet ? "" : "error");
      return;
    }
    var validation = analyzeBase64(input);
    if (!validation.valid) {
      var invalidNotes = validation.diagnostics.concat(validation.warnings);
      setOutput(form, "Invalid Base64\n" + validation.message + (invalidNotes.length ? "\n" + invalidNotes.join("\n") : ""));
      setMessage(form, "Invalid Base64 input.", "error");
      setFeedback(form, validation.details, invalidNotes, "error");
      setPreview(form, "", "");
      setAdvanced(form, "");
      form._lastResult = {
        type: "validation",
        text: form.querySelector("[data-tool-output]").value,
        extension: "txt",
        mime: "text/plain;charset=utf-8"
      };
      return;
    }
    var report = validationReport(validation);
    setOutput(form, report);
    setMessage(form, validation.message, validation.warnings.length > 0 ? "warning" : "success");
    setFeedback(form, validation.details, validation.warnings, validation.warnings.length > 0 ? "warning" : "success");
    setPreview(form, "", "");
    setAdvanced(form, hexSection(validation.bytes, "Decoded byte preview"));
    form._lastResult = {
      type: "validation",
      text: report,
      extension: "txt",
      mime: "text/plain;charset=utf-8"
    };
  }

  function validationReport(validation) {
    var lines = ["Valid Base64"];
    validation.details.forEach(function (detail) {
      lines.push(detail[0] + ": " + detail[1]);
    });
    if (validation.warnings.length > 0) {
      lines.push("");
      lines.push("Notes:");
      validation.warnings.forEach(function (warning) {
        lines.push("- " + warning);
      });
    }
    return lines.join("\n");
  }

  function previewForBytes(bytes, text, textStatus) {
    var media = detectMedia(bytes, text, textStatus);
    if (media.kind === "image") {
      return {
        kind: "image",
        title: media.label,
        html: "<img class=\"preview-image\" alt=\"" + escapeHtml(media.label) + "\" src=\"data:" + media.mime + ";base64," + bytesToBase64(bytes) + "\">",
        outputText: media.label + " detected. Use Download result to save " + formatBytes(bytes.length) + ".",
        extension: media.extension,
        mime: media.mime
      };
    }
    if (media.kind === "pdf") {
      return {
        kind: "pdf",
        title: "PDF document detected",
        html: "<p>PDF document detected. Size: " + escapeHtml(formatBytes(bytes.length)) + ".</p>",
        outputText: "PDF document detected. Use Download result to save " + formatBytes(bytes.length) + ".",
        extension: "pdf",
        mime: "application/pdf"
      };
    }
    if (media.kind === "json") {
      return {
        kind: "json",
        title: "Formatted JSON preview",
        html: "<pre><code>" + escapeHtml(media.formatted) + "</code></pre>",
        outputText: media.formatted,
        extension: "json",
        mime: "application/json;charset=utf-8"
      };
    }
    if (media.kind === "svg") {
      return {
        kind: "svg",
        title: "SVG image preview",
        html: "<img class=\"preview-image\" alt=\"SVG preview\" src=\"data:image/svg+xml;base64," + bytesToBase64(bytes) + "\">",
        outputText: text,
        extension: "svg",
        mime: "image/svg+xml;charset=utf-8"
      };
    }
    if (textStatus === "UTF-8 text") {
      return {
        kind: "text",
        title: "Text preview",
        html: "<pre><code>" + escapeHtml(text.slice(0, 4000)) + "</code></pre>",
        outputText: text,
        extension: "txt",
        mime: "text/plain;charset=utf-8"
      };
    }
    return {
      kind: "binary",
      title: "Binary preview",
      html: "<pre class=\"hex-preview\">" + escapeHtml(hexPreview(bytes)) + "</pre>",
      outputText: "Decoded binary data. Use Download result to save " + formatBytes(bytes.length) + ".",
      extension: media.extension || "bin",
      mime: media.mime || "application/octet-stream"
    };
  }

  function detectMedia(bytes, text, textStatus) {
    if (startsWith(bytes, [0x89, 0x50, 0x4e, 0x47])) {
      return { kind: "image", label: "PNG image preview", extension: "png", mime: "image/png" };
    }
    if (startsWith(bytes, [0xff, 0xd8, 0xff])) {
      return { kind: "image", label: "JPEG image preview", extension: "jpg", mime: "image/jpeg" };
    }
    if (startsWithAscii(bytes, "GIF87a") || startsWithAscii(bytes, "GIF89a")) {
      return { kind: "image", label: "GIF image preview", extension: "gif", mime: "image/gif" };
    }
    if (startsWithAscii(bytes, "RIFF") && asciiAt(bytes, 8, 4) === "WEBP") {
      return { kind: "image", label: "WebP image preview", extension: "webp", mime: "image/webp" };
    }
    if (startsWithAscii(bytes, "%PDF")) {
      return { kind: "pdf", extension: "pdf", mime: "application/pdf" };
    }
    if (textStatus === "UTF-8 text") {
      var trimmed = text.trim();
      if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
        try {
          return { kind: "json", formatted: JSON.stringify(JSON.parse(trimmed), null, 2), extension: "json", mime: "application/json;charset=utf-8" };
        } catch (error) {
          // Keep falling through to text preview.
        }
      }
      if (/^(<\?xml[\s\S]*?)?<svg[\s>]/i.test(trimmed)) {
        return { kind: "svg", extension: "svg", mime: "image/svg+xml;charset=utf-8" };
      }
    }
    return { kind: "binary", extension: "bin", mime: "application/octet-stream" };
  }

  function startsWith(bytes, prefix) {
    if (bytes.length < prefix.length) {
      return false;
    }
    return prefix.every(function (value, index) {
      return bytes[index] === value;
    });
  }

  function startsWithAscii(bytes, value) {
    return asciiAt(bytes, 0, value.length) === value;
  }

  function asciiAt(bytes, start, length) {
    var result = "";
    for (var index = 0; index < length && start + index < bytes.length; index++) {
      result += String.fromCharCode(bytes[start + index]);
    }
    return result;
  }

  function hexSection(bytes, title) {
    return "<div class=\"preview-title\">" + escapeHtml(title) + "</div><pre class=\"hex-preview\">" + escapeHtml(hexPreview(bytes)) + "</pre>";
  }

  function hexPreview(bytes) {
    var limit = Math.min(bytes.length, 64);
    var lines = [];
    for (var offset = 0; offset < limit; offset += 16) {
      var slice = bytes.subarray(offset, Math.min(offset + 16, limit));
      var hex = Array.from(slice).map(function (byte) {
        return byte.toString(16).padStart(2, "0");
      }).join(" ");
      var ascii = Array.from(slice).map(function (byte) {
        return byte >= 32 && byte <= 126 ? String.fromCharCode(byte) : ".";
      }).join("");
      lines.push(offset.toString(16).padStart(4, "0") + "  " + hex.padEnd(47, " ") + "  " + ascii);
    }
    if (bytes.length > limit) {
      lines.push("... " + formatBytes(bytes.length - limit) + " more");
    }
    return lines.join("\n");
  }

  function copyResult(form) {
    var output = form.querySelector("[data-tool-output]");
    if (!output || !output.value) {
      setMessage(form, "There is no output to copy yet.", "error");
      return;
    }
    if (navigator.clipboard && navigator.clipboard.writeText) {
      navigator.clipboard.writeText(output.value).then(function () {
        setMessage(form, "Copied result.", "success");
      }).catch(function () {
        output.select();
        document.execCommand("copy");
        setMessage(form, "Copied result.", "success");
      });
      return;
    }
    output.select();
    document.execCommand("copy");
    setMessage(form, "Copied result.", "success");
  }

  function downloadResult(form) {
    var result = form._lastResult;
    var output = form.querySelector("[data-tool-output]");
    if (!result && (!output || !output.value)) {
      setMessage(form, "There is no output to download yet.", "error");
      return;
    }
    var extension = result && result.extension ? result.extension : "txt";
    var mime = result && result.mime ? result.mime : "text/plain;charset=utf-8";
    var content = result && result.bytes && result.type === "decodedBinary" ? result.bytes : (result && result.text ? result.text : output.value);
    var blob = new Blob([content], { type: mime });
    var action = form.dataset.activeAction || form.dataset.capability || "result";
    var link = document.createElement("a");
    link.href = URL.createObjectURL(blob);
    link.download = "validohub-base64-" + action + "." + extension;
    document.body.appendChild(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(link.href);
    setMessage(form, "Downloaded result.", "success");
  }

  function clearForm(form) {
    form._file = null;
    form._lastResult = null;
    form.querySelectorAll("textarea[name], input[name]").forEach(function (field) {
      if (field.type === "checkbox") {
        field.checked = field.defaultChecked;
      } else if (field.type !== "file") {
        field.value = "";
      }
    });
    form.querySelectorAll("input[type=\"file\"]").forEach(function (field) {
      field.value = "";
    });
    form.querySelectorAll("select[name]").forEach(function (field) {
      field.selectedIndex = 0;
    });
    setOutput(form, "");
    clearPanels(form);
    setMessage(form, "", "");
    updateFileStatus(form, null);
    updateBadge(form);
  }

  function markActiveAction(form, action) {
    form.dataset.activeAction = action;
    form.querySelectorAll("[data-action]").forEach(function (button) {
      var active = button.dataset.action === action;
      button.classList.toggle("is-active", active);
      button.setAttribute("aria-pressed", active ? "true" : "false");
    });
  }

  function debounce(callback, delay) {
    var timer = 0;
    return function () {
      window.clearTimeout(timer);
      timer = window.setTimeout(callback, delay);
    };
  }

  function updateBadge(form) {
    var input = primaryInput(form);
    setBadge(form, detectInputMode(input ? input.value : ""));
  }

  function sample(form, sampleId) {
    var input = primaryInput(form);
    if (!input) {
      return;
    }
    form._file = null;
    updateFileStatus(form, null);
    if (sampleId === "encode-hello") {
      input.value = "Hello, world!";
      markActiveAction(form, "encode");
      runAction(form, "encode");
      return;
    }
    if (sampleId === "encode-unicode") {
      input.value = "Hello, こんにちは, 👋";
      markActiveAction(form, "encode");
      runAction(form, "encode");
      return;
    }
    if (sampleId === "decode") {
      input.value = "SGVsbG8sIHdvcmxkIQ==";
      markActiveAction(form, "decode");
      runAction(form, "decode");
      return;
    }
    if (sampleId === "validate") {
      var validateForm = document.querySelector(".tool-workbench[data-algorithm-id=\"validohub.base64\"][data-capability=\"validate\"]");
      var validateInput = validateForm ? primaryInput(validateForm) : null;
      if (validateForm && validateInput) {
        validateInput.value = "eyJzdGF0dXMiOiJvayIsImNvdW50IjoyfQ==";
        markActiveAction(validateForm, "validate");
        updateBadge(validateForm);
        runAction(validateForm, "validate");
        validateForm.scrollIntoView({ behavior: "smooth", block: "center" });
      }
    }
  }

  function enhanceFileInput(form) {
    var dropzone = form.querySelector("[data-file-dropzone]");
    var input = form.querySelector("[data-file-input]");
    if (!dropzone || !input || !window.FileReader) {
      return;
    }
    input.addEventListener("change", function () {
      if (input.files && input.files[0]) {
        readFile(form, input.files[0]);
      }
    });
    ["dragenter", "dragover"].forEach(function (name) {
      dropzone.addEventListener(name, function (event) {
        event.preventDefault();
        dropzone.classList.add("is-dragging");
      });
    });
    ["dragleave", "drop"].forEach(function (name) {
      dropzone.addEventListener(name, function (event) {
        event.preventDefault();
        dropzone.classList.remove("is-dragging");
      });
    });
    dropzone.addEventListener("drop", function (event) {
      var file = event.dataTransfer && event.dataTransfer.files && event.dataTransfer.files[0];
      if (file) {
        readFile(form, file);
      }
    });
  }

  function readFile(form, file) {
    var reader = new FileReader();
    reader.onload = function () {
      form._file = {
        name: file.name || "unnamed-file",
        size: file.size || reader.result.byteLength,
        type: file.type || "",
        bytes: new Uint8Array(reader.result)
      };
      var input = primaryInput(form);
      if (input) {
        input.value = "";
      }
      markActiveAction(form, "encode");
      updateFileStatus(form, form._file);
      setBadge(form, { label: "File bytes ready", state: "base64" });
      runAction(form, "encode");
    };
    reader.onerror = function () {
      setMessage(form, "Could not read file.", "error");
    };
    reader.readAsArrayBuffer(file);
  }

  function updateFileStatus(form, file) {
    var status = form.querySelector("[data-file-status]");
    if (!status) {
      return;
    }
    status.textContent = file
        ? file.name + " - " + formatBytes(file.bytes.length) + (file.type ? " - " + file.type : "")
        : "No upload. File bytes stay in this browser.";
  }

  function enhanceBase64Form(form) {
    form.classList.add("base64-workbench");
    var actionButtons = Array.from(form.querySelectorAll("[data-action]"));
    var initialAction = actionButtons.length > 0 ? actionButtons[0].dataset.action : form.dataset.capability;
    markActiveAction(form, initialAction);
    updateBadge(form);
    actionButtons.forEach(function (button) {
      button.setAttribute("aria-keyshortcuts", "Control+Enter Meta+Enter");
    });
    var clearButton = form.querySelector("[data-tool-clear]");
    if (clearButton) {
      clearButton.setAttribute("aria-keyshortcuts", "Escape");
    }
    enhanceFileInput(form);
    var liveRun = debounce(function () {
      updateBadge(form);
      runAction(form, form.dataset.activeAction || initialAction, { quiet: true });
    }, 180);
    form.querySelectorAll("textarea[name], input[name], select[name]").forEach(function (field) {
      if (field.type === "file") {
        return;
      }
      field.addEventListener("input", function () {
        if (field.tagName === "TEXTAREA") {
          form._file = null;
          updateFileStatus(form, null);
        }
        liveRun();
      });
      field.addEventListener("change", liveRun);
      field.addEventListener("keydown", function (event) {
        if ((event.ctrlKey || event.metaKey) && event.key === "Enter") {
          event.preventDefault();
          runAction(form, form.dataset.activeAction || initialAction);
        }
        if (event.key === "Escape") {
          event.preventDefault();
          clearForm(form);
        }
      });
    });
  }

  document.querySelectorAll(".tool-workbench").forEach(function (form) {
    if (form.dataset.algorithmId === BASE64_ALGORITHM) {
      enhanceBase64Form(form);
    }
    form.addEventListener("click", function (event) {
      var actionButton = event.target.closest("[data-action]");
      if (actionButton) {
        markActiveAction(form, actionButton.dataset.action);
        runAction(form, actionButton.dataset.action);
        return;
      }
      var sampleButton = event.target.closest("[data-sample]");
      if (sampleButton) {
        sample(form, sampleButton.dataset.sample);
        return;
      }
      if (event.target.closest("[data-tool-copy]")) {
        copyResult(form);
        return;
      }
      if (event.target.closest("[data-tool-download]")) {
        downloadResult(form);
        return;
      }
      if (event.target.closest("[data-tool-clear]")) {
        clearForm(form);
      }
    });
  });
})();
