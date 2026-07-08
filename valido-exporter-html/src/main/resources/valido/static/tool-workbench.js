(function () {
  var BASE64_ALGORITHM = "validohub.base64";
  var URL_ENCODER_ALGORITHM = "validohub.url-encoder";
  var URL_DECODER_ALGORITHM = "validohub.url-decoder";
  var BASE64_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=_-";

  var ValidoWorkbench = (function () {
    var textEncoder = new TextEncoder();
    var textDecoder = new TextDecoder("utf-8", { fatal: true });
    var plugins = {};

    function registerPlugin(algorithmId, plugin) {
      plugins[algorithmId] = plugin;
    }

    function mountAll() {
      document.querySelectorAll(".tool-workbench").forEach(function (form) {
        var plugin = plugins[form.dataset.algorithmId];
        if (plugin) {
          new Workbench(form, plugin).mount();
        }
      });
    }

    function Workbench(form, plugin) {
      this.form = form;
      this.plugin = plugin;
      this.file = null;
      this.lastResult = null;
    }

    Workbench.prototype.mount = function () {
      var workbench = this;
      var actionButtons = Array.from(this.form.querySelectorAll("[data-action]"));
      var initialAction = actionButtons.length > 0 ? actionButtons[0].dataset.action : this.form.dataset.capability;
      this.form.classList.add("browser-workbench");
      this.markActiveAction(initialAction);
      this.updateBadge();
      actionButtons.forEach(function (button) {
        button.setAttribute("aria-keyshortcuts", "Control+Enter Meta+Enter");
      });
      var clearButton = this.form.querySelector("[data-tool-clear]");
      if (clearButton) {
        clearButton.setAttribute("aria-keyshortcuts", "Escape");
      }
      this.bindLiveMode(initialAction);
      this.bindFileInput();
      this.form.addEventListener("click", function (event) {
        var actionButton = event.target.closest("[data-action]");
        if (actionButton) {
          workbench.markActiveAction(actionButton.dataset.action);
          workbench.run(actionButton.dataset.action);
          return;
        }
        var sampleButton = event.target.closest("[data-sample]");
        if (sampleButton) {
          workbench.plugin.applySample(workbench, sampleButton.dataset.sample);
          workbench.updateBadge();
          return;
        }
        if (event.target.closest("[data-tool-copy]")) {
          workbench.copy();
          return;
        }
        if (event.target.closest("[data-tool-download]")) {
          workbench.download();
          return;
        }
        if (event.target.closest("[data-tool-clear]")) {
          workbench.clear();
        }
      });
      if (this.plugin.onMount) {
        this.plugin.onMount(this);
      }
    };

    Workbench.prototype.bindLiveMode = function (initialAction) {
      var workbench = this;
      var liveRun = debounce(function () {
        workbench.updateBadge();
        workbench.run(workbench.form.dataset.activeAction || initialAction, { quiet: true });
      }, 180);
      this.form.querySelectorAll("textarea[name], input[name], select[name]").forEach(function (field) {
        if (field.type === "file") {
          return;
        }
        field.addEventListener("input", function () {
          if (field.tagName === "TEXTAREA") {
            workbench.file = null;
            workbench.updateFileStatus(null);
          }
          liveRun();
        });
        field.addEventListener("change", liveRun);
        field.addEventListener("keydown", function (event) {
          if ((event.ctrlKey || event.metaKey) && event.key === "Enter") {
            event.preventDefault();
            workbench.run(workbench.form.dataset.activeAction || initialAction);
          }
          if (event.key === "Escape") {
            event.preventDefault();
            workbench.clear();
          }
        });
      });
    };

    Workbench.prototype.bindFileInput = function () {
      var workbench = this;
      var dropzone = this.form.querySelector("[data-file-dropzone]");
      var input = this.form.querySelector("[data-file-input]");
      if (!dropzone || !input || !window.FileReader || !this.plugin.handleFile) {
        return;
      }
      input.addEventListener("change", function () {
        if (input.files && input.files[0]) {
          workbench.readFile(input.files[0]);
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
          workbench.readFile(file);
        }
      });
    };

    Workbench.prototype.readFile = function (file) {
      var workbench = this;
      var reader = new FileReader();
      reader.onload = function () {
        workbench.file = {
          name: file.name || "unnamed-file",
          size: file.size || reader.result.byteLength,
          type: file.type || "",
          bytes: new Uint8Array(reader.result)
        };
        var input = workbench.primaryInput();
        if (input) {
          input.value = "";
        }
        workbench.markActiveAction("encode");
        workbench.updateFileStatus(workbench.file);
        workbench.setBadge({ label: "File bytes ready", state: "base64" });
        workbench.plugin.handleFile(workbench, workbench.file);
      };
      reader.onerror = function () {
        workbench.setMessage("Could not read file.", "error");
      };
      reader.readAsArrayBuffer(file);
    };

    Workbench.prototype.run = function (action, options) {
      this.plugin.run(this, action, options || {});
    };

    Workbench.prototype.values = function () {
      var values = {};
      this.form.querySelectorAll("textarea[name], input[name], select[name]").forEach(function (field) {
        if (field.type === "checkbox") {
          values[field.name] = field.checked;
        } else {
          values[field.name] = field.value;
        }
      });
      return values;
    };

    Workbench.prototype.primaryInput = function () {
      return this.form.querySelector("textarea[name=\"input\"]");
    };

    Workbench.prototype.setMessage = function (text, state) {
      var message = this.form.querySelector("[data-tool-message]");
      if (!message) {
        return;
      }
      message.textContent = text || "";
      message.dataset.state = state || "";
    };

    Workbench.prototype.setOutput = function (value) {
      var output = this.form.querySelector("[data-tool-output]");
      if (output) {
        output.value = value || "";
      }
    };

    Workbench.prototype.outputValue = function () {
      var output = this.form.querySelector("[data-tool-output]");
      return output ? output.value : "";
    };

    Workbench.prototype.setBadge = function (mode) {
      var badge = this.form.querySelector("[data-input-mode-badge]");
      if (!badge) {
        return;
      }
      badge.textContent = mode.label;
      badge.dataset.state = mode.state || "";
    };

    Workbench.prototype.updateBadge = function () {
      var input = this.primaryInput();
      this.setBadge(this.plugin.detectInputMode ? this.plugin.detectInputMode(input ? input.value : "") : { label: "Waiting for input", state: "" });
    };

    Workbench.prototype.setStats = function (details, notes, state) {
      var feedback = this.form.querySelector("[data-tool-feedback]");
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
    };

    Workbench.prototype.setPreview = function (title, bodyHtml) {
      var preview = this.form.querySelector("[data-tool-preview]");
      if (!preview) {
        return;
      }
      preview.innerHTML = title ? "<div class=\"preview-title\">" + escapeHtml(title) + "</div><div class=\"preview-body\">" + bodyHtml + "</div>" : "";
    };

    Workbench.prototype.setAdvanced = function (html) {
      var advanced = this.form.querySelector("[data-advanced-panel]");
      var target = this.form.querySelector("[data-tool-advanced]");
      if (!advanced || !target) {
        return;
      }
      target.innerHTML = html || "";
      advanced.classList.toggle("has-content", Boolean(html));
    };

    Workbench.prototype.clearPanels = function () {
      this.setStats([], [], "");
      this.setPreview("", "");
      this.setAdvanced("");
    };

    Workbench.prototype.markActiveAction = function (action) {
      this.form.dataset.activeAction = action;
      this.form.querySelectorAll("[data-action]").forEach(function (button) {
        var active = button.dataset.action === action;
        button.classList.toggle("is-active", active);
        button.setAttribute("aria-pressed", active ? "true" : "false");
      });
    };

    Workbench.prototype.copy = function () {
      var output = this.form.querySelector("[data-tool-output]");
      var workbench = this;
      if (!output || !output.value) {
        this.setMessage("There is no output to copy yet.", "error");
        return;
      }
      copyText(output.value, function () {
        output.select();
        document.execCommand("copy");
      }).then(function () {
        workbench.setMessage("Copied result.", "success");
      });
    };

    Workbench.prototype.download = function () {
      var result = this.lastResult;
      var output = this.form.querySelector("[data-tool-output]");
      if (!result && (!output || !output.value)) {
        this.setMessage("There is no output to download yet.", "error");
        return;
      }
      var action = this.form.dataset.activeAction || this.form.dataset.capability || "result";
      downloadResult((this.plugin.filePrefix || "validohub-tool") + "-" + action, result, output ? output.value : "");
      this.setMessage("Downloaded result.", "success");
    };

    Workbench.prototype.clear = function () {
      this.file = null;
      this.lastResult = null;
      this.form.querySelectorAll("textarea[name], input[name]").forEach(function (field) {
        if (field.type === "checkbox") {
          field.checked = field.defaultChecked;
        } else if (field.type !== "file") {
          field.value = "";
        }
      });
      this.form.querySelectorAll("input[type=\"file\"]").forEach(function (field) {
        field.value = "";
      });
      this.form.querySelectorAll("select[name]").forEach(function (field) {
        field.selectedIndex = 0;
      });
      this.setOutput("");
      this.clearPanels();
      this.setMessage("", "");
      this.updateFileStatus(null);
      this.updateBadge();
    };

    Workbench.prototype.updateFileStatus = function (file) {
      var status = this.form.querySelector("[data-file-status]");
      if (!status) {
        return;
      }
      status.textContent = file
          ? file.name + " - " + formatBytes(file.bytes.length) + (file.type ? " - " + file.type : "")
          : "No upload. File bytes stay in this browser.";
    };

    function copyText(value, fallback) {
      if (navigator.clipboard && navigator.clipboard.writeText) {
        return navigator.clipboard.writeText(value).catch(function () {
          fallback();
        });
      }
      fallback();
      return Promise.resolve();
    }

    function downloadResult(baseName, result, fallbackText) {
      var extension = result && result.extension ? result.extension : "txt";
      var mime = result && result.mime ? result.mime : "text/plain;charset=utf-8";
      var content = result && result.bytes && result.type === "decodedBinary" ? result.bytes : (result && result.text ? result.text : fallbackText);
      var blob = new Blob([content], { type: mime });
      var link = document.createElement("a");
      link.href = URL.createObjectURL(blob);
      link.download = baseName + "." + extension;
      document.body.appendChild(link);
      link.click();
      link.remove();
      URL.revokeObjectURL(link.href);
    }

    function utf8Bytes(value) {
      return textEncoder.encode(value);
    }

    function utf8Text(bytes) {
      return textDecoder.decode(bytes);
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

    function hexSection(bytes, title) {
      return "<div class=\"preview-title\">" + escapeHtml(title) + "</div><pre class=\"hex-preview\">" + escapeHtml(hexPreview(bytes)) + "</pre>";
    }

    function escapeHtml(value) {
      return String(value)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;");
    }

    function debounce(callback, delay) {
      var timer = 0;
      return function () {
        window.clearTimeout(timer);
        timer = window.setTimeout(callback, delay);
      };
    }

    return {
      registerPlugin: registerPlugin,
      mountAll: mountAll,
      utilities: {
        debounce: debounce,
        escapeHtml: escapeHtml,
        formatBytes: formatBytes,
        formatRatio: formatRatio,
        hexPreview: hexPreview,
        hexSection: hexSection,
        utf8Bytes: utf8Bytes,
        utf8Text: utf8Text
      }
    };
  })();

  var Base64Plugin = (function (framework) {
    var util = framework.utilities;

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
        text = util.utf8Text(decoded.bytes);
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
      details.push(["Input UTF-8 bytes", util.formatBytes(util.utf8Bytes(raw).length)]);
      details.push(["Output characters", textStatus === "UTF-8 text" ? String(Array.from(text).length) : "Binary output"]);
      details.push(["Decoded byte size", util.formatBytes(decoded.bytes.length)]);
      details.push(["Estimated decoded size", util.formatBytes(estimatedDecodedSize(compact))]);
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
        message: "Valid " + variant + ". Decoded size: " + util.formatBytes(decoded.bytes.length) + ".",
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

    function run(workbench, action, options) {
      var values = workbench.values();
      var quiet = options && options.quiet;

      if (workbench.form.dataset.capability === "encode" && action === "encode") {
        if (workbench.file) {
          encodeFile(workbench, workbench.file, values);
          return;
        }
        if (!values.input || !values.input.trim()) {
          workbench.setOutput("");
          workbench.clearPanels();
          workbench.setMessage(quiet ? "" : "Enter text to encode.", quiet ? "" : "error");
          return;
        }
        var inputBytes = util.utf8Bytes(values.input);
        var output = encodeBytes(inputBytes, {
          urlSafe: Boolean(values.urlSafe),
          padding: values.padding !== false
        });
        workbench.setOutput(output);
        workbench.setMessage("Encoded live.", "success");
        workbench.setStats(encodeDetails(values.input, inputBytes, output, values), [], "success");
        workbench.setPreview("", "");
        workbench.setAdvanced(util.hexSection(inputBytes, "Input byte preview"));
        workbench.lastResult = {
          type: "encodedText",
          text: output,
          extension: "txt",
          mime: "text/plain;charset=utf-8"
        };
        return;
      }

      if (workbench.form.dataset.capability === "encode" && action === "decode") {
        decodeIntoWorkbench(workbench, values.input, quiet);
        return;
      }

      if (workbench.form.dataset.capability === "validate" && action === "validate") {
        validateIntoWorkbench(workbench, values.input, quiet);
      }
    }

    function handleFile(workbench, file) {
      var values = workbench.values();
      encodeFile(workbench, file, values);
    }

    function encodeFile(workbench, file, values) {
      var output = encodeBytes(file.bytes, {
        urlSafe: Boolean(values.urlSafe),
        padding: values.padding !== false
      });
      var details = [
        ["File name", file.name],
        ["File size", util.formatBytes(file.bytes.length)],
        ["MIME type", file.type || "Unknown"],
        ["Output characters", output.length + " characters"],
        ["Variant", values.urlSafe ? "Base64URL" : "Standard Base64"],
        ["Padding", values.padding !== false ? "Included" : "Omitted"],
        ["Expansion ratio", util.formatRatio(file.bytes.length, output.length)]
      ];
      workbench.setOutput(output);
      workbench.setMessage("Encoded file locally.", "success");
      workbench.setStats(details, ["File bytes were read locally in this browser only."], "success");
      workbench.setPreview("", "");
      workbench.setAdvanced(util.hexSection(file.bytes, "File byte preview"));
      workbench.lastResult = {
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
        ["Input UTF-8 bytes", util.formatBytes(inputBytes.length)],
        ["Output characters", output.length + " characters"],
        ["Decoded byte size", util.formatBytes(inputBytes.length)],
        ["Estimated decoded size", util.formatBytes(inputBytes.length)],
        ["Variant", values.urlSafe ? "Base64URL" : "Standard Base64"],
        ["Padding", values.padding !== false ? "Included" : paddingState(output, inputBytes.length, true)],
        ["Expansion ratio", util.formatRatio(inputBytes.length, output.length)],
        ["Contains whitespace", /\s/.test(input) ? "Yes" : "No"],
        ["Canonical", "Yes"]
      ];
    }

    function decodeIntoWorkbench(workbench, input, quiet) {
      if (!input || !input.trim()) {
        workbench.setOutput("");
        workbench.clearPanels();
        workbench.setMessage(quiet ? "" : "Enter Base64 text to decode.", quiet ? "" : "error");
        return;
      }
      var decoded = analyzeBase64(input);
      if (!decoded.valid) {
        workbench.setOutput("");
        workbench.setMessage(decoded.message, "error");
        workbench.setStats(decoded.details, decoded.diagnostics.concat(decoded.warnings), "error");
        workbench.setPreview("", "");
        workbench.setAdvanced("");
        return;
      }
      var preview = previewForBytes(decoded.bytes, decoded.text, decoded.textStatus);
      if (decoded.textStatus === "UTF-8 text") {
        workbench.setOutput(preview.outputText);
      } else {
        workbench.setOutput("Decoded binary data. Use Download result to save " + util.formatBytes(decoded.bytes.length) + ".");
      }
      workbench.setMessage("Decoded live.", decoded.warnings.length > 0 ? "warning" : "success");
      workbench.setStats(decoded.details, decoded.warnings, decoded.warnings.length > 0 ? "warning" : "success");
      workbench.setPreview(preview.title, preview.html);
      workbench.setAdvanced(util.hexSection(decoded.bytes, "Decoded byte preview"));
      workbench.lastResult = {
        type: preview.kind === "text" || preview.kind === "json" || preview.kind === "svg" ? "decodedText" : "decodedBinary",
        text: preview.outputText,
        bytes: decoded.bytes,
        extension: preview.extension,
        mime: preview.mime
      };
    }

    function validateIntoWorkbench(workbench, input, quiet) {
      if (!input || !input.trim()) {
        workbench.setOutput("");
        workbench.clearPanels();
        workbench.setMessage(quiet ? "" : "Enter Base64 text to validate.", quiet ? "" : "error");
        return;
      }
      var validation = analyzeBase64(input);
      if (!validation.valid) {
        var invalidNotes = validation.diagnostics.concat(validation.warnings);
        workbench.setOutput("Invalid Base64\n" + validation.message + (invalidNotes.length ? "\n" + invalidNotes.join("\n") : ""));
        workbench.setMessage("Invalid Base64 input.", "error");
        workbench.setStats(validation.details, invalidNotes, "error");
        workbench.setPreview("", "");
        workbench.setAdvanced("");
        workbench.lastResult = {
          type: "validation",
          text: workbench.outputValue(),
          extension: "txt",
          mime: "text/plain;charset=utf-8"
        };
        return;
      }
      var report = validationReport(validation);
      workbench.setOutput(report);
      workbench.setMessage(validation.message, validation.warnings.length > 0 ? "warning" : "success");
      workbench.setStats(validation.details, validation.warnings, validation.warnings.length > 0 ? "warning" : "success");
      workbench.setPreview("", "");
      workbench.setAdvanced(util.hexSection(validation.bytes, "Decoded byte preview"));
      workbench.lastResult = {
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
          html: "<img class=\"preview-image\" alt=\"" + util.escapeHtml(media.label) + "\" src=\"data:" + media.mime + ";base64," + bytesToBase64(bytes) + "\">",
          outputText: media.label + " detected. Use Download result to save " + util.formatBytes(bytes.length) + ".",
          extension: media.extension,
          mime: media.mime
        };
      }
      if (media.kind === "pdf") {
        return {
          kind: "pdf",
          title: "PDF document detected",
          html: "<p>PDF document detected. Size: " + util.escapeHtml(util.formatBytes(bytes.length)) + ".</p>",
          outputText: "PDF document detected. Use Download result to save " + util.formatBytes(bytes.length) + ".",
          extension: "pdf",
          mime: "application/pdf"
        };
      }
      if (media.kind === "json") {
        return {
          kind: "json",
          title: "Formatted JSON preview",
          html: "<pre><code>" + util.escapeHtml(media.formatted) + "</code></pre>",
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
          html: "<pre><code>" + util.escapeHtml(text.slice(0, 4000)) + "</code></pre>",
          outputText: text,
          extension: "txt",
          mime: "text/plain;charset=utf-8"
        };
      }
      return {
        kind: "binary",
        title: "Binary preview",
        html: "<pre class=\"hex-preview\">" + util.escapeHtml(util.hexPreview(bytes)) + "</pre>",
        outputText: "Decoded binary data. Use Download result to save " + util.formatBytes(bytes.length) + ".",
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

    function applySample(workbench, sampleId) {
      var input = workbench.primaryInput();
      if (!input) {
        return;
      }
      workbench.file = null;
      workbench.updateFileStatus(null);
      if (sampleId === "encode-hello") {
        input.value = "Hello, world!";
        workbench.markActiveAction("encode");
        workbench.run("encode");
        return;
      }
      if (sampleId === "encode-unicode") {
        input.value = "Hello, こんにちは, 👋";
        workbench.markActiveAction("encode");
        workbench.run("encode");
        return;
      }
      if (sampleId === "decode") {
        input.value = "SGVsbG8sIHdvcmxkIQ==";
        workbench.markActiveAction("decode");
        workbench.run("decode");
        return;
      }
      if (sampleId === "validate") {
        var validateForm = document.querySelector(".tool-workbench[data-algorithm-id=\"validohub.base64\"][data-capability=\"validate\"]");
        if (validateForm && validateForm._workbench) {
          var validateInput = validateForm._workbench.primaryInput();
          if (validateInput) {
            validateInput.value = "eyJzdGF0dXMiOiJvayIsImNvdW50IjoyfQ==";
            validateForm._workbench.markActiveAction("validate");
            validateForm._workbench.updateBadge();
            validateForm._workbench.run("validate");
            if (validateForm.scrollIntoView) {
              validateForm.scrollIntoView({ behavior: "smooth", block: "center" });
            }
          }
        }
      }
    }

    function onMount(workbench) {
      workbench.form._workbench = workbench;
    }

    return {
      filePrefix: "validohub-base64",
      onMount: onMount,
      run: run,
      handleFile: handleFile,
      applySample: applySample,
      detectInputMode: detectInputMode
    };
  })(ValidoWorkbench);

  var UrlPlugin = (function (framework) {
    var util = framework.utilities;
    var RESERVED = ":/?#[]@!$&'()*+,;=";
    var UNRESERVED = /^[A-Za-z0-9._~-]$/;

    function run(workbench, action, options) {
      var values = workbench.values();
      var input = values.input || "";
      var quiet = options && options.quiet;
      if (!input.trim()) {
        workbench.setOutput("");
        workbench.clearPanels();
        workbench.setMessage(quiet ? "" : "Enter text or URL-encoded input.", quiet ? "" : "error");
        return;
      }
      if (action === "encode") {
        encode(workbench, input);
        return;
      }
      if (action === "decode") {
        decode(workbench, input);
        return;
      }
      if (action === "validate") {
        validate(workbench, input);
      }
    }

    function encode(workbench, input) {
      var output = encodeURIComponent(input);
      var analysis = analyzeUrlEncoding(input, output, "encode");
      workbench.setOutput(output);
      workbench.setMessage("Encoded live.", analysis.warnings.length > 0 ? "warning" : "success");
      workbench.setStats(analysis.details, analysis.warnings, analysis.warnings.length > 0 ? "warning" : "success");
      workbench.setPreview("Space handling", spacePreview(input, output));
      workbench.setAdvanced(advancedReport(analysis));
      workbench.lastResult = textResult(output);
    }

    function decode(workbench, input) {
      var validation = validatePercentEncoding(input);
      if (!validation.valid) {
        showInvalid(workbench, input, validation);
        return;
      }
      try {
        var output = decodeURIComponent(input);
        var analysis = analyzeUrlEncoding(input, output, "decode");
        workbench.setOutput(output);
        workbench.setMessage("Decoded live.", analysis.warnings.length > 0 ? "warning" : "success");
        workbench.setStats(analysis.details, analysis.warnings, analysis.warnings.length > 0 ? "warning" : "success");
        workbench.setPreview("Decoded preview", "<pre><code>" + util.escapeHtml(output.slice(0, 4000)) + "</code></pre>");
        workbench.setAdvanced(advancedReport(analysis));
        workbench.lastResult = textResult(output);
      } catch (error) {
        showInvalid(workbench, input, {
          valid: false,
          diagnostics: ["Malformed UTF-8 percent-encoded byte sequence."],
          invalidSequences: collectInvalidSequences(input)
        });
      }
    }

    function validate(workbench, input) {
      var validation = validatePercentEncoding(input);
      if (!validation.valid) {
        showInvalid(workbench, input, validation);
        workbench.lastResult = textResult(workbench.outputValue());
        return;
      }
      var decoded = "";
      var warnings = [];
      try {
        decoded = decodeURIComponent(input);
      } catch (error) {
        warnings.push("Percent escapes are shaped correctly, but decoded bytes are not valid UTF-8.");
      }
      var analysis = analyzeUrlEncoding(input, decoded || input, "validate");
      var notes = analysis.warnings.concat(warnings);
      var report = validationReport(analysis, notes);
      workbench.setOutput(report);
      workbench.setMessage("Valid URL encoding.", notes.length > 0 ? "warning" : "success");
      workbench.setStats(analysis.details, notes, notes.length > 0 ? "warning" : "success");
      workbench.setPreview("", "");
      workbench.setAdvanced(advancedReport(analysis));
      workbench.lastResult = textResult(report);
    }

    function showInvalid(workbench, input, validation) {
      var details = baseDetails(input, "", "validate");
      var diagnostics = validation.diagnostics || ["Malformed URL encoding."];
      workbench.setOutput("Invalid URL encoding\n" + diagnostics.join("\n"));
      workbench.setMessage("Invalid URL encoding.", "error");
      workbench.setStats(details, diagnostics, "error");
      workbench.setPreview("Invalid sequence highlight", highlightInvalidSequences(input, validation.invalidSequences || []));
      workbench.setAdvanced("<div class=\"preview-title\">Diagnostics</div><ul class=\"feedback-notes\">" + diagnostics.map(function (diagnostic) {
        return "<li>" + util.escapeHtml(diagnostic) + "</li>";
      }).join("") + "</ul>");
      workbench.lastResult = textResult(workbench.outputValue());
    }

    function validatePercentEncoding(input) {
      var diagnostics = [];
      var invalidSequences = collectInvalidSequences(input);
      invalidSequences.forEach(function (item) {
        diagnostics.push("Invalid percent sequence '" + item.sequence + "' at position " + item.position + ".");
      });
      return {
        valid: diagnostics.length === 0,
        diagnostics: diagnostics,
        invalidSequences: invalidSequences
      };
    }

    function collectInvalidSequences(input) {
      var invalid = [];
      for (var index = 0; index < input.length; index++) {
        if (input.charAt(index) !== "%") {
          continue;
        }
        var sequence = invalidPercentSequence(input, index);
        if (index + 2 >= input.length || !/^[0-9A-Fa-f]{2}$/.test(input.slice(index + 1, index + 3))) {
          invalid.push({
            position: index + 1,
            sequence: sequence
          });
        }
      }
      return invalid;
    }

    function invalidPercentSequence(input, index) {
      if (index + 2 >= input.length) {
        return input.slice(index);
      }
      var first = input.charAt(index + 1);
      var second = input.charAt(index + 2);
      if (/^[0-9A-Fa-f]$/.test(first) && !/^[0-9A-Fa-f]$/.test(second)) {
        return input.slice(index, index + 2);
      }
      return input.slice(index, index + 3);
    }

    function analyzeUrlEncoding(input, output, mode) {
      var encodedSide = mode === "encode" ? output : input;
      var decodedSide = mode === "encode" ? input : output;
      var details = baseDetails(input, output, mode);
      var warnings = [];
      var validation = validatePercentEncoding(input);
      if (!validation.valid) {
        warnings = warnings.concat(validation.diagnostics);
      }
      if (/%[0-9A-Fa-f]{2}/.test(input) && mode === "encode") {
        warnings.push("Input already contains percent-encoded sequences. Encoding again will escape the percent signs.");
      }
      if (/\+/.test(input) && mode !== "encode") {
        warnings.push("Plus signs are preserved. This decoder does not treat + as a space.");
      }
      if (/\s/.test(decodedSide)) {
        warnings.push("Space handling: spaces are encoded as %20.");
      }
      details.push(["Character count", String(Array.from(input).length)]);
      details.push(["Encoded length", encodedSide.length + " characters"]);
      details.push(["Decoded length", decodedSide ? Array.from(decodedSide).length + " characters" : "n/a"]);
      details.push(["Percent-encoded byte count", String(percentByteCount(encodedSide))]);
      details.push(["Reserved character count", String(countReserved(decodedSide || input))]);
      details.push(["Unsafe character count", String(countUnsafe(decodedSide || input))]);
      details.push(["Contains spaces", /\s/.test(decodedSide || input) ? "Yes" : "No"]);
      details.push(["Already encoded", /%[0-9A-Fa-f]{2}/.test(input) ? "Yes" : "No"]);
      return {
        details: details,
        warnings: warnings,
        encodedSide: encodedSide,
        decodedSide: decodedSide
      };
    }

    function baseDetails(input, output, mode) {
      return [
        ["Mode", mode],
        ["Input UTF-8 bytes", util.formatBytes(util.utf8Bytes(input).length)],
        ["Output UTF-8 bytes", output ? util.formatBytes(util.utf8Bytes(output).length) : "n/a"]
      ];
    }

    function percentByteCount(value) {
      return (value.match(/%[0-9A-Fa-f]{2}/g) || []).length;
    }

    function countReserved(value) {
      return Array.from(value || "").filter(function (character) {
        return RESERVED.indexOf(character) !== -1;
      }).length;
    }

    function countUnsafe(value) {
      return Array.from(value || "").filter(function (character) {
        return !UNRESERVED.test(character);
      }).length;
    }

    function spacePreview(input, output) {
      var count = (input.match(/\s/g) || []).length;
      if (count === 0) {
        return "<p>No spaces detected. Existing characters were percent-encoded where needed.</p>";
      }
      return "<p>" + count + " space character" + (count === 1 ? "" : "s") + " encoded as <code>%20</code>.</p>"
          + "<pre><code>" + util.escapeHtml(output) + "</code></pre>";
    }

    function highlightInvalidSequences(input, invalidSequences) {
      if (invalidSequences.length === 0) {
        return "<pre><code>" + util.escapeHtml(input) + "</code></pre>";
      }
      var invalidByPosition = {};
      invalidSequences.forEach(function (item) {
        invalidByPosition[item.position - 1] = item.sequence.length;
      });
      var html = "";
      for (var index = 0; index < input.length;) {
        if (invalidByPosition[index]) {
          var sequence = input.slice(index, index + invalidByPosition[index]);
          html += "<mark class=\"invalid-sequence\">" + util.escapeHtml(sequence) + "</mark>";
          index += invalidByPosition[index];
        } else {
          html += util.escapeHtml(input.charAt(index));
          index++;
        }
      }
      return "<pre><code>" + html + "</code></pre>";
    }

    function advancedReport(analysis) {
      return "<div class=\"preview-title\">URL encoding analysis</div><dl class=\"feedback-grid\">"
          + analysis.details.map(function (row) {
            return "<div><dt>" + util.escapeHtml(row[0]) + "</dt><dd>" + util.escapeHtml(row[1]) + "</dd></div>";
          }).join("")
          + "</dl>";
    }

    function validationReport(analysis, notes) {
      var lines = ["Valid URL encoding"];
      analysis.details.forEach(function (detail) {
        lines.push(detail[0] + ": " + detail[1]);
      });
      if (notes.length > 0) {
        lines.push("");
        lines.push("Notes:");
        notes.forEach(function (note) {
          lines.push("- " + note);
        });
      }
      return lines.join("\n");
    }

    function detectInputMode(value) {
      if (!value || !value.trim()) {
        return { label: "Waiting for input", state: "" };
      }
      var validation = validatePercentEncoding(value);
      if (!validation.valid) {
        return { label: "Malformed URL encoding", state: "invalid" };
      }
      if (/%[0-9A-Fa-f]{2}/.test(value)) {
        return { label: "Already encoded", state: "base64" };
      }
      return { label: "Plain text", state: "text" };
    }

    function applySample(workbench, sampleId) {
      var input = workbench.primaryInput();
      if (!input) {
        return;
      }
      if (sampleId === "url-hello") {
        input.value = "Hello World!";
        workbench.markActiveAction("encode");
        workbench.run("encode");
        return;
      }
      if (sampleId === "url-unicode") {
        input.value = "Café こんにちは 👋";
        workbench.markActiveAction("encode");
        workbench.run("encode");
        return;
      }
      if (sampleId === "url-encoded") {
        input.value = "https%3A%2F%2Fexample.com%2Fsearch%3Fq%3Dhello%2520world";
        workbench.markActiveAction("decode");
        workbench.run("decode");
        return;
      }
      if (sampleId === "url-malformed") {
        input.value = "hello%2 world%ZZ";
        workbench.markActiveAction("validate");
        workbench.run("validate");
      }
    }

    function onMount(workbench) {
      workbench.form._workbench = workbench;
    }

    function textResult(value) {
      return {
        type: "text",
        text: value,
        extension: "txt",
        mime: "text/plain;charset=utf-8"
      };
    }

    return {
      filePrefix: "validohub-url",
      onMount: onMount,
      run: run,
      applySample: applySample,
      detectInputMode: detectInputMode
    };
  })(ValidoWorkbench);

  window.ValidoWorkbench = ValidoWorkbench;
  ValidoWorkbench.registerPlugin(BASE64_ALGORITHM, Base64Plugin);
  ValidoWorkbench.registerPlugin(URL_ENCODER_ALGORITHM, UrlPlugin);
  ValidoWorkbench.registerPlugin(URL_DECODER_ALGORITHM, UrlPlugin);
  ValidoWorkbench.mountAll();
})();
