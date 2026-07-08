(function () {
  var BASE64_ALGORITHM = "validohub.base64";

  function bytesToBase64(bytes) {
    var binary = "";
    var chunkSize = 0x8000;
    for (var i = 0; i < bytes.length; i += chunkSize) {
      var chunk = bytes.subarray(i, i + chunkSize);
      binary += String.fromCharCode.apply(null, chunk);
    }
    return window.btoa(binary);
  }

  function encodeBase64(value, options) {
    var bytes = new TextEncoder().encode(value);
    var result = bytesToBase64(bytes);
    if (options.urlSafe) {
      result = result.replace(/\+/g, "-").replace(/\//g, "_");
    }
    if (!options.padding) {
      result = result.replace(/=+$/g, "");
    }
    return {
      value: result,
      byteLength: bytes.length,
      inputCharacters: Array.from(value).length,
      variant: options.urlSafe ? "Base64URL" : "Standard Base64",
      padding: options.padding ? "Included" : "Omitted"
    };
  }

  function normalizeBase64(value) {
    var compact = value.replace(/\s+/g, "");
    var normalized = compact.replace(/-/g, "+").replace(/_/g, "/");
    var remainder = normalized.length % 4;
    if (remainder === 1) {
      throw new Error("Base64 length cannot leave a remainder of 1.");
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
      throw new Error("Use only Base64 characters with padding at the end.");
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
    return new TextDecoder("utf-8", { fatal: true }).decode(bytes);
  }

  function analyzeBase64(value) {
    var raw = value || "";
    var compact = raw.replace(/\s+/g, "");
    var details = [];
    var warnings = [];

    if (!compact) {
      return {
        valid: false,
        message: "Enter Base64 text.",
        details: [],
        warnings: []
      };
    }

    if (/[^A-Za-z0-9+/=_-]/.test(compact)) {
      return invalid("Invalid character found. Base64 allows letters, numbers, +, /, -, _, and =.", details);
    }

    var paddingIndex = compact.indexOf("=");
    if (paddingIndex !== -1 && !/^=+$/.test(compact.slice(paddingIndex))) {
      return invalid("Padding must appear only at the end.", details);
    }

    var paddingCount = (compact.match(/=/g) || []).length;
    if (paddingCount > 2) {
      return invalid("Base64 padding can use at most two = characters.", details);
    }

    var hasUrlSafe = /[-_]/.test(compact);
    var hasStandardSpecials = /[+/]/.test(compact);
    if (hasUrlSafe && hasStandardSpecials) {
      warnings.push("Input mixes standard and URL-safe alphabets.");
    }

    if (paddingCount > 0 && compact.length % 4 !== 0) {
      return invalid("Padded Base64 length should be a multiple of 4.", details);
    }

    var decoded;
    try {
      decoded = base64ToBytes(compact);
    } catch (error) {
      return invalid(error.message, details);
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
    var variant = hasUrlSafe ? "Base64URL" : "Standard Base64";
    var padding = paddingCount > 0 ? "Present" : "Omitted";
    var ignoredWhitespace = raw.length - compact.length;

    details.push(["Variant", variant]);
    details.push(["Padding", padding]);
    details.push(["Encoded length", compact.length + " characters"]);
    details.push(["Decoded size", formatBytes(decoded.bytes.length)]);
    details.push(["Decoded type", textStatus]);
    if (ignoredWhitespace > 0) {
      details.push(["Whitespace ignored", String(ignoredWhitespace) + " characters"]);
    }
    if (compact !== canonical && compact !== canonical.replace(/=+$/g, "")
        && compact !== canonicalUrl && compact !== canonicalUrl.replace(/=+$/g, "")) {
      warnings.push("Input is valid but not in canonical padded form.");
    }

    return {
      valid: true,
      bytes: decoded.bytes,
      text: text,
      textStatus: textStatus,
      canonical: canonical,
      canonicalUrl: canonicalUrl,
      message: "Valid " + variant + ". Decoded size: " + formatBytes(decoded.bytes.length) + ".",
      details: details,
      warnings: warnings
    };
  }

  function invalid(message, details) {
    return {
      valid: false,
      message: message,
      details: details || [],
      warnings: []
    };
  }

  function formatBytes(count) {
    if (count === 1) {
      return "1 byte";
    }
    return count + " bytes";
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

  function setFeedback(form, details, warnings, state) {
    var feedback = form.querySelector("[data-tool-feedback]");
    if (!feedback) {
      return;
    }
    var rows = details || [];
    var notes = warnings || [];
    if (rows.length === 0 && notes.length === 0) {
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
    if (notes.length > 0) {
      html += "<ul class=\"feedback-notes\">";
      notes.forEach(function (note) {
        html += "<li>" + escapeHtml(note) + "</li>";
      });
      html += "</ul>";
    }
    feedback.innerHTML = html;
    feedback.dataset.state = state || "";
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
      if (!values.input || !values.input.trim()) {
        setOutput(form, "");
        setFeedback(form, [], [], "");
        setMessage(form, quiet ? "" : "Enter text to encode.", quiet ? "" : "error");
        return;
      }
      var encoded = encodeBase64(values.input, {
        urlSafe: Boolean(values.urlSafe),
        padding: values.padding !== false
      });
      setOutput(form, encoded.value);
      setMessage(form, "Encoded live.", "success");
      setFeedback(form, [
        ["Input", encoded.inputCharacters + " characters"],
        ["UTF-8 size", formatBytes(encoded.byteLength)],
        ["Output length", encoded.value.length + " characters"],
        ["Variant", encoded.variant],
        ["Padding", encoded.padding]
      ], [], "success");
      return;
    }

    if (algorithmId === BASE64_ALGORITHM && capability === "encode" && action === "decode") {
      if (!values.input || !values.input.trim()) {
        setOutput(form, "");
        setFeedback(form, [], [], "");
        setMessage(form, quiet ? "" : "Enter Base64 text to decode.", quiet ? "" : "error");
        return;
      }
      var decoded = analyzeBase64(values.input);
      if (!decoded.valid) {
        setOutput(form, "");
        setMessage(form, decoded.message, "error");
        setFeedback(form, decoded.details, decoded.warnings, "error");
        return;
      }
      if (decoded.textStatus !== "UTF-8 text") {
        setOutput(form, "");
        setMessage(form, "Valid Base64, but the decoded bytes are not UTF-8 text.", "error");
        setFeedback(form, decoded.details, decoded.warnings, "error");
        return;
      }
      setOutput(form, decoded.text);
      setMessage(form, "Decoded live.", "success");
      setFeedback(form, decoded.details, decoded.warnings, decoded.warnings.length > 0 ? "warning" : "success");
      return;
    }

    if (algorithmId === BASE64_ALGORITHM && capability === "validate" && action === "validate") {
      if (!values.input || !values.input.trim()) {
        setOutput(form, "");
        setFeedback(form, [], [], "");
        setMessage(form, quiet ? "" : "Enter Base64 text to validate.", quiet ? "" : "error");
        return;
      }
      var validation = analyzeBase64(values.input);
      if (!validation.valid) {
        setOutput(form, "Invalid Base64\n" + validation.message);
        setMessage(form, "Invalid Base64 input.", "error");
        setFeedback(form, validation.details, [validation.message].concat(validation.warnings), "error");
        return;
      }
      setOutput(form, validationReport(validation));
      setMessage(form, validation.message, validation.warnings.length > 0 ? "warning" : "success");
      setFeedback(form, validation.details, validation.warnings, validation.warnings.length > 0 ? "warning" : "success");
      return;
    }

    setMessage(form, "Unable to run this action.", "error");
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
    var output = form.querySelector("[data-tool-output]");
    if (!output || !output.value) {
      setMessage(form, "There is no output to download yet.", "error");
      return;
    }
    var blob = new Blob([output.value], { type: "text/plain;charset=utf-8" });
    var link = document.createElement("a");
    var action = form.dataset.activeAction || form.dataset.capability || "result";
    link.href = URL.createObjectURL(blob);
    link.download = "validohub-base64-" + action + ".txt";
    document.body.appendChild(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(link.href);
    setMessage(form, "Downloaded result.", "success");
  }

  function clearForm(form) {
    form.querySelectorAll("textarea[name], input[name]").forEach(function (field) {
      if (field.type === "checkbox") {
        field.checked = field.defaultChecked;
      } else {
        field.value = "";
      }
    });
    form.querySelectorAll("select[name]").forEach(function (field) {
      field.selectedIndex = 0;
    });
    setOutput(form, "");
    setFeedback(form, [], [], "");
    setMessage(form, "", "");
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

  function enhanceBase64Form(form) {
    form.classList.add("base64-workbench");
    var actionButtons = Array.from(form.querySelectorAll("[data-action]"));
    var initialAction = actionButtons.length > 0 ? actionButtons[0].dataset.action : form.dataset.capability;
    markActiveAction(form, initialAction);
    actionButtons.forEach(function (button) {
      button.setAttribute("aria-keyshortcuts", "Control+Enter Meta+Enter");
    });
    var clearButton = form.querySelector("[data-tool-clear]");
    if (clearButton) {
      clearButton.setAttribute("aria-keyshortcuts", "Escape");
    }
    var liveRun = debounce(function () {
      runAction(form, form.dataset.activeAction || initialAction, { quiet: true });
    }, 180);
    form.querySelectorAll("textarea[name], input[name], select[name]").forEach(function (field) {
      field.addEventListener("input", liveRun);
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
