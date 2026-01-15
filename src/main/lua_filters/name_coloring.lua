function bxor(a, b)
    local result = 0
    local bit = 1
    while a > 0 or b > 0 do
        local a_bit = a % 2
        local b_bit = b % 2
        if a_bit ~= b_bit then
            result = result + bit
        end
        a = math.floor(a / 2)
        b = math.floor(b / 2)
        bit = bit * 2
    end
    return result
end

-- FNV-1a-style 32-bit hash, good avalanche on last characters
function string_hash(str)
    local hash = 2166136261 -- offset basis
    local FNV_prime = 16777619
    local MOD32 = 4294967296

    for i = 1, #str do
        local c = str:byte(i)
        hash = bxor(hash, c)
        hash = (hash * FNV_prime) % MOD32
    end

    return hash
end

-- HSL -> RGB conversion.
-- h, s, l in [0,1]
function hsl_to_rgb(h, s, l)
    local r, g, b

    if s == 0 then
        r, g, b = l, l, l
    else
        local function hue2rgb(p, q, t)
            if t < 0 then t = t + 1 end
            if t > 1 then t = t - 1 end
            if t < 1/6 then return p + (q - p) * 6 * t end
            if t < 1/2 then return q end
            if t < 2/3 then return p + (q - p) * (2/3 - t) * 6 end
            return p
        end

        local q
        if l < 0.5 then
            q = l * (1 + s)
        else
            q = l + s - l * s
        end
        local p = 2 * l - q

        r = hue2rgb(p, q, h + 1/3)
        g = hue2rgb(p, q, h)
        b = hue2rgb(p, q, h - 1/3)
    end

    r = math.floor(r * 255 + 0.5)
    g = math.floor(g * 255 + 0.5)
    b = math.floor(b * 255 + 0.5)

    return r, g, b
end

-- Map name -> high-contrast, fairly bright color (hex).
-- Black text will be readable on these backgrounds.
function generate_latex_highlight(name)
    local hash = string_hash(name)

    -- Use different parts of the hash for H, S, L to reduce clustering.
    local h  = (hash % 360) / 360 -- hue over full circle

    local s_byte = math.floor(hash / 360) % 256
    local l_byte = math.floor(hash / (360 * 256)) % 256

    -- Saturation: 0.55–0.90  (fairly vivid, so colors differ clearly)
    local s = 0.65 + (s_byte / 255) * 0.35

    -- Lightness: 0.50–0.75  (bright enough for black text)
    local l = 0.60 + (l_byte / 255) * 0.25

    local r, g, b = hsl_to_rgb(h, s, l)
    return string.format("[RGB]{%d, %d, %d}", r, g, b)
end

function Str(el)
    -- Only act if the word starts with @
    if el.text:sub(1, 1) == '@' then

        -- Remove the leading @
        local content = el.text:sub(2)

        -- Match the ID and the Punctuation
        -- [%w%-_] matches Alphanumeric characters, hyphens, and underscores
        local id, punctuation = content:match("^([%w%-_]+)(.*)$")

        if id then
            -- 1. Generate color based on the ID (e.g. "Jonas_R")
            local color_spec = generate_latex_highlight(id:lower())

            -- 2. Create the display name by swapping Underscore for Space
            local display_name = id:gsub("_", " ")

            -- 3. Generate LaTeX
            local latex_code = string.format(
                "\\colorbox%s{\\strut %s}",
                color_spec,
                display_name
            )

            -- 4. Return the result (preserving punctuation outside the box)
            if punctuation and punctuation ~= "" then
                return {
                    pandoc.RawInline('latex', latex_code),
                    pandoc.Str(punctuation)
                }
            else
                return pandoc.RawInline('latex', latex_code)
            end
        end
    end
    -- Return nil ensures we don't touch normal text
    return nil
end