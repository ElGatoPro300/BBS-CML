#version 150

#moj_import <fog.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in vec2 UV1;
in ivec2 UV2;
in vec3 Normal;

uniform mat4 ModelViewMat;
uniform mat4 FormRootInverse;
uniform mat4 FogMat;
uniform mat4 ProjMat;
uniform int FogShape;
uniform mat3 IViewRotMat;

out float vertexDistance;
out vec4 vertexColor;
out vec2 texCoord0;
out vec3 formRootPos;

float bbs_vertex_fog_distance(vec3 pos, int shape, mat3 iViewRot)
{
    if (shape == 0)
    {
        return length(pos);
    }
    else
    {
        vec3 camRel = iViewRot * pos;
        return max(length(camRel.xz), abs(camRel.y));
    }
}

void main()
{
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    vertexDistance = bbs_vertex_fog_distance(Position, FogShape, IViewRotMat);
    vertexColor = Color;
    texCoord0 = UV0;
    formRootPos = (FormRootInverse * vec4(Position, 1.0)).xyz;
}
