/*
 * Copyright LWJGL. All rights reserved.
 * License terms: https://www.lwjgl.org/license
 */
#version 110

attribute vec4 aVertex;

uniform mat4 model;
uniform mat4 view;
uniform mat4 proj;

varying vec3 worldNormal;

void main(void) {
	vec4 modelPosition = model * aVertex * vec4(0.1, 0.1, 0.1, 1.0);
  	worldNormal = gl_Normal;
  	vPosition = modelPosition.xyz;
  	gl_Position = proj * view * model * gl_Vertex;
}
