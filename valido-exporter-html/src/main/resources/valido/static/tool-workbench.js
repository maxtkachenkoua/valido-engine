(function () {
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
    return result;
  }

  function normalizeBase64(value) {
    var normalized = value.replace(/\s+/g, "").replace(/-/g, "+").replace(/_/g, "/");
    var remainder = normalized.length % 4;
    if (remainder === 1) {
      throw new Error("Invalid Base64 length.");
    }
    if (remainder > 0) {
      normalized += "=".repeat(4 - remainder);
    }
    return normalized;
  }

  function base64ToBytes(value) {
    var normalized = normalizeBase64(value);
    if (!/^[A-Za-z0-9+/]*={0,2}$/.test(normalized) || /=[^=]/.test(normalized)) {
      throw new Error("Use only Base64 characters with padding at the end.");
    }
    var binary = window.atob(normalized);
    var bytes = new Uint8Array(binary.length);
    for (var i = 0; i < binary.length; i++) {
      bytes[i] = binary.charCodeAt(i);
    }
    return bytes;
  }

  function decodeBase64(value) {
    return new TextDecoder("utf-8", { fatal: true }).decode(base64ToBytes(value));
  }

  function validateBase64(value) {
    var bytes = base64ToBytes(value);
    return "Valid Base64. Decoded byte length: " + bytes.length + ".";
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

  function runAction(form, action) {
    var algorithmId = form.dataset.algorithmId;
    var capability = form.dataset.capability;
    var values = formValues(form);

    if (algorithmId === "validohub.base64" && capability === "encode" && action === "encode") {
      if (!values.input || !values.input.trim()) {
        setOutput(form, "");
        setMessage(form, "Enter text to encode.", "error");
        return;
      }
      setOutput(form, encodeBase64(values.input, {
        urlSafe: Boolean(values.urlSafe),
        padding: values.padding !== false
      }));
      setMessage(form, "Encoded successfully.", "success");
      return;
    }

    if (algorithmId === "validohub.base64" && capability === "encode" && action === "decode") {
      if (!values.input || !values.input.trim()) {
        setOutput(form, "");
        setMessage(form, "Enter Base64 text to decode.", "error");
        return;
      }
      try {
        setOutput(form, decodeBase64(values.input));
        setMessage(form, "Decoded successfully.", "success");
      } catch (error) {
        setOutput(form, "");
        setMessage(form, "Enter valid UTF-8 Base64 text.", "error");
      }
      return;
    }

    if (algorithmId === "validohub.base64" && capability === "validate" && action === "validate") {
      if (!values.input || !values.input.trim()) {
        setOutput(form, "");
        setMessage(form, "Enter Base64 text to validate.", "error");
        return;
      }
      try {
        setOutput(form, validateBase64(values.input));
        setMessage(form, "Validation passed.", "success");
      } catch (error) {
        setOutput(form, "");
        setMessage(form, "Invalid Base64 input.", "error");
      }
      return;
    }

    setMessage(form, "Unable to run this action.", "error");
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
    setMessage(form, "", "");
  }

  document.querySelectorAll(".tool-workbench").forEach(function (form) {
    form.addEventListener("click", function (event) {
      var actionButton = event.target.closest("[data-action]");
      if (actionButton) {
        runAction(form, actionButton.dataset.action);
        return;
      }
      if (event.target.closest("[data-tool-copy]")) {
        copyResult(form);
        return;
      }
      if (event.target.closest("[data-tool-clear]")) {
        clearForm(form);
      }
    });
  });
})();
