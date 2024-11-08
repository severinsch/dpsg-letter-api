function parse_yaml_block(yaml_text)
    -- Escape single quotes in yaml_text by replacing ' with '"'"'
    yaml_text = yaml_text:gsub("'", "'\"'\"'")

    -- Use `echo` to pipe `yaml_text` into `yq` for YAML-to-JSON conversion, rely ony yq to preserve order
    local command = "echo '" .. yaml_text .. "' | yq"
    local handle = io.popen(command)
    local json_text = handle:read("*a")
    handle:close()

    -- Extract keys in order from the JSON string, necessary as JSON decoding does not preserve order
    local ordered_keys = {}
    for key in json_text:gmatch('"([^"]+)":') do
        table.insert(ordered_keys, key)
    end

    -- Decode JSON text into a Lua table
    local metadata = pandoc.json.decode(json_text)

    -- Return both ordered keys and metadata table
    return ordered_keys, metadata
end

function format_value_for_latex(value)
    if type(value) == "table" then
        -- Join list items with a comma and space for LaTeX formatting
        return table.concat(value, ", ")
    else
        local formatted_value = tostring(value)
        -- Remove the trailing newline if it exists
        if formatted_value:sub(-1) == "\n" then
            formatted_value = formatted_value:sub(1, -2)
        end
        -- Replace remaining newlines with LaTeX newline command
        return tostring(formatted_value):gsub("\n", "\\newline ")
    end
end

function insert_metadata_table(ordered_keys, metadata)
    local tabular_str = "\\begin{tabular}{lp{\\columnwidth}}\n"
    for _, key in ipairs(ordered_keys) do
        local value = metadata[key]
        local formatted_value = format_value_for_latex(value)
        tabular_str = tabular_str .. key .. ": &\\textbf{" .. formatted_value .. "}\\\\\n"
    end
    tabular_str = tabular_str .. "\\end{tabular}"
    return pandoc.RawBlock("latex", tabular_str)
end

function Block(block)
    -- Detect the YAML block and replace it
    if block.t == "CodeBlock" and block.classes[1] == "table" then
        local ordered_keys, metadata = parse_yaml_block(block.text)
        return insert_metadata_table(ordered_keys, metadata)
    end
end
