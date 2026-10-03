#version 150

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform vec2 TexelSize;
uniform float Thickness;
uniform vec4 OutlineMaskColor;
uniform float Rainbow;
uniform float RainbowSpeed;
uniform float RainbowScale;
uniform float GameTime;

in vec2 texCoord0;

out vec4 fragColor;

vec3 hsv2rgb(vec3 c)
{
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);

    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

void main()
{
    vec4 self = texture(Sampler1, texCoord0);

    if (self.g > 0.5)
    {
        discard;
    }

    int radius = int(ceil(Thickness));

    if (radius <= 0)
    {
        discard;
    }

    float bestDist = 1.0e6;
    float bestDepth = 1.0;
    bool found = false;

    for (int y = -radius; y <= radius; y++)
    {
        float vdist = abs(float(y));

        if (vdist > Thickness)
        {
            continue;
        }

        vec2 uv = texCoord0 + vec2(0.0, float(y)) * TexelSize;
        vec4 sampleData = texture(Sampler0, uv);

        /* Negative .g means no mask pixel on this row within range */
        if (sampleData.g < 0.0)
        {
            continue;
        }

        float dist = length(vec2(sampleData.g, vdist));

        if (dist <= Thickness && dist < bestDist)
        {
            bestDist = dist;
            bestDepth = sampleData.r;
            found = true;
        }
    }

    if (!found)
    {
        discard;
    }

    float edgeFade = clamp(Thickness - bestDist + 1.0, 0.0, 1.0);

    vec3 color = OutlineMaskColor.rgb;

    if (Rainbow > 0.5)
    {
        float aspect = TexelSize.y / max(TexelSize.x, 0.00001);
        float coord = (texCoord0.x * aspect + texCoord0.y) * (RainbowScale * 2.0) - GameTime * (RainbowSpeed * 0.5);

        color = hsv2rgb(vec3(fract(coord), 1.0, 1.0));
    }

    fragColor = vec4(color, OutlineMaskColor.a * edgeFade);
}
