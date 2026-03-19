-- tables-inline-tabular.lua
--
-- Inline, non-floating LaTeX tables for Pandoc.
-- Good for two-column documents where longtable is not allowed and floats
-- are undesirable.
--
-- Supports:
-- - simple markdown tables
-- - captions (inline, non-floating)
-- - labels/identifiers
-- - alignment
-- - multiple table bodies
--
-- Intentionally falls back to Pandoc default handling for:
-- - row spans / column spans
-- - complex block content inside cells
-- - unusual table structures

local DEFAULT_WIDTH = "\\columnwidth"

local ALIGN_SIMPLE = {
  AlignLeft    = "l",
  AlignRight   = "r",
  AlignCenter  = "c",
  AlignDefault = "l",
}

local ALIGN_P = {
  AlignLeft    = [[\raggedright\arraybackslash]],
  AlignRight   = [[\raggedleft\arraybackslash]],
  AlignCenter  = [[\centering\arraybackslash]],
  AlignDefault = [[\raggedright\arraybackslash]],
}

local function trim(s)
  return (s:gsub("^%s+", ""):gsub("%s+$", ""))
end

local function get_attr(elem, key)
  if elem.attr and elem.attr.attributes and elem.attr.attributes[key] ~= nil then
    return elem.attr.attributes[key]
  end
  if elem.attributes and elem.attributes[key] ~= nil then
    return elem.attributes[key]
  end
  return nil
end

local function has_class(elem, class_name)
  local classes = {}
  if elem.attr and elem.attr.classes then
    classes = elem.attr.classes
  elseif elem.classes then
    classes = elem.classes
  end
  for _, c in ipairs(classes) do
    if c == class_name then
      return true
    end
  end
  return false
end

local function get_identifier(elem)
  if elem.attr and elem.attr.identifier and elem.attr.identifier ~= "" then
    return elem.attr.identifier
  end
  if elem.identifier and elem.identifier ~= "" then
    return elem.identifier
  end
  return nil
end

local function blocks_to_latex(blocks)
  if not blocks or #blocks == 0 then
    return ""
  end
  local tex = pandoc.write(pandoc.Pandoc(blocks), "latex")
  tex = trim(tex)
  tex = tex:gsub("\\par%s*$", "")
  return trim(tex)
end

local function inlines_to_latex(inlines)
  if not inlines or #inlines == 0 then
    return ""
  end
  return blocks_to_latex({ pandoc.Plain(inlines) })
end

local function caption_to_latex(tbl)
  if not tbl.caption then
    return "", ""
  end

  local long_caption = ""
  local short_caption = ""

  if tbl.caption.long then
    long_caption = blocks_to_latex(tbl.caption.long)
  elseif type(tbl.caption) == "table" then
    long_caption = blocks_to_latex(tbl.caption)
  end

  if tbl.caption.short then
    short_caption = inlines_to_latex(tbl.caption.short)
  end

  return trim(long_caption), trim(short_caption)
end

local function section_rows(section)
  if not section then
    return {}
  end
  if section.rows then
    return section.rows
  end
  return section
end

local function cell_row_span(cell)
  return cell.row_span or cell.rowspan or 1
end

local function cell_col_span(cell)
  return cell.col_span or cell.colspan or 1
end

local function simple_cell_content_to_latex(cell)
  local parts = {}

  for _, blk in ipairs(cell.contents or {}) do
    if blk.t == "Plain" or blk.t == "Para" then
      local tex = trim(blocks_to_latex({ blk }))
      if tex ~= "" then
        parts[#parts + 1] = tex
      end
    else
      return nil
    end
  end

  if #parts == 0 then
    return ""
  elseif #parts == 1 then
    return parts[1]
  else
    return [[\makecell[l]{]] .. table.concat(parts, [[ \\ ]]) .. [[}]]
  end
end

local function row_to_latex(row)
  local out = {}
  for _, cell in ipairs(row.cells or {}) do
    if cell_row_span(cell) ~= 1 or cell_col_span(cell) ~= 1 then
      return nil
    end

    local tex = simple_cell_content_to_latex(cell)
    if tex == nil then
      return nil
    end
    out[#out + 1] = tex
  end
  return table.concat(out, " & ") .. [[ \\]]
end

local function colspec_to_latex(colspecs, total_width_cmd)
  local specs = {}
  local saw_explicit_width = false

  for _, spec in ipairs(colspecs or {}) do
    local align = spec[1]
    local width = spec[2]

    if type(width) == "number" and width > 0 then
      saw_explicit_width = true
      local frac = math.min(width, 0.98)
      local ragged = ALIGN_P[align] or ALIGN_P.AlignDefault
      specs[#specs + 1] = string.format(">{%s}p{%.4f%s}", ragged, frac, total_width_cmd)
    else
      specs[#specs + 1] = ALIGN_SIMPLE[align] or ALIGN_SIMPLE.AlignDefault
    end
  end

  return table.concat(specs, ""), saw_explicit_width
end

local function render_inline_table(tbl)
  local label = get_identifier(tbl)
  local caption_long, caption_short = caption_to_latex(tbl)

  local width_cmd = get_attr(tbl, "latex-width")
  if not width_cmd or width_cmd == "" then
    width_cmd = DEFAULT_WIDTH
  end

  local colspec = colspec_to_latex(tbl.colspecs or {}, width_cmd)

  local lines = {}
  lines[#lines + 1] = "\\par"
  lines[#lines + 1] = "\\medskip"
  lines[#lines + 1] = "\\noindent"
  lines[#lines + 1] = "\\begin{minipage}{\\columnwidth}"
  lines[#lines + 1] = "\\centering"

  if caption_long ~= "" then
    if caption_short ~= "" then
      lines[#lines + 1] = string.format("\\captionof{table}[%s]{%s}", caption_short, caption_long)
    else
      lines[#lines + 1] = string.format("\\captionof{table}{%s}", caption_long)
    end
    if label then
      lines[#lines + 1] = string.format("\\label{%s}", label)
    end
    lines[#lines + 1] = "\\smallskip"
  elseif label then
    lines[#lines + 1] = "\\refstepcounter{table}"
    lines[#lines + 1] = string.format("\\label{%s}", label)
  end

  lines[#lines + 1] = string.format("\\begin{tabular}{@{}%s@{}}", colspec)
  lines[#lines + 1] = "\\toprule"

  local head_rows = section_rows(tbl.head)
  if #head_rows > 0 then
    for _, row in ipairs(head_rows) do
      local tex = row_to_latex(row)
      if tex == nil then
        return nil
      end
      lines[#lines + 1] = tex
    end
    lines[#lines + 1] = "\\midrule"
  end

  for _, body in ipairs(tbl.bodies or {}) do
    local body_head = section_rows(body.head)
    local body_rows = section_rows(body.body)

    if #body_head > 0 then
      for _, row in ipairs(body_head) do
        local tex = row_to_latex(row)
        if tex == nil then
          return nil
        end
        lines[#lines + 1] = tex
      end
      lines[#lines + 1] = "\\midrule"
    end

    for _, row in ipairs(body_rows) do
      local tex = row_to_latex(row)
      if tex == nil then
        return nil
      end
      lines[#lines + 1] = tex
    end
  end

  local foot_rows = section_rows(tbl.foot)
  if #foot_rows > 0 then
    lines[#lines + 1] = "\\midrule"
    for _, row in ipairs(foot_rows) do
      local tex = row_to_latex(row)
      if tex == nil then
        return nil
      end
      lines[#lines + 1] = tex
    end
  end

  lines[#lines + 1] = "\\bottomrule"
  lines[#lines + 1] = "\\end{tabular}"
  lines[#lines + 1] = "\\end{minipage}"
  lines[#lines + 1] = "\\par"
  lines[#lines + 1] = "\\medskip"

  return pandoc.RawBlock("latex", table.concat(lines, "\n"))
end

function Table(tbl)
  if FORMAT ~= "latex" then
    return nil
  end

  -- Optional escape hatch:
  -- add class "longtable" to let Pandoc handle a table normally.
  if has_class(tbl, "longtable") then
    return nil
  end

  local rendered = render_inline_table(tbl)
  if rendered == nil then
    -- Complex table: let Pandoc do its default thing.
    return nil
  end
  return rendered
end