#version 300 es

uniform mat4 u_ModelView;
uniform mat4 u_ModelViewProjection;

layout(location = 0) in vec4 a_Position;
layout(location = 1) in vec3 a_Normal;

out vec3 Normal;


void main() {
    gl_Position = u_ModelViewProjection * vec4(a_Position.xyz, 1.0);
    Normal = normalize(mat3(transpose(inverse(u_ModelView))) * a_Normal);
}
