/*
 * Copyright LWJGL. All rights reserved.
 * License terms: https://www.lwjgl.org/license
 */
package org.lwjgl.demo.opengl.assimp;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.*;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.assimp.Assimp.*;
import static org.lwjgl.demo.opengl.util.DemoUtils.*;
import static org.lwjgl.glfw.GLFW.*;

import static org.lwjgl.opengl.ARBFragmentShader.GL_FRAGMENT_SHADER_ARB;
import static org.lwjgl.opengl.ARBSeamlessCubeMap.GL_TEXTURE_CUBE_MAP_SEAMLESS;
import static org.lwjgl.opengl.ARBShaderObjects.*;
import static org.lwjgl.opengl.ARBVertexBufferObject.*;
import static org.lwjgl.opengl.ARBVertexShader.GL_VERTEX_SHADER_ARB;
import static org.lwjgl.opengl.ARBVertexShader.glEnableVertexAttribArrayARB;
import static org.lwjgl.opengl.ARBVertexShader.glGetAttribLocationARB;
import static org.lwjgl.opengl.GL11.GL_LINEAR;
import static org.lwjgl.opengl.GL11.GL_LINEAR_MIPMAP_LINEAR;
import static org.lwjgl.opengl.GL11.GL_RGB;
import static org.lwjgl.opengl.GL11.GL_RGB8;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TRUE;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glEnableClientState;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL11.glVertexPointer;
import static org.lwjgl.opengl.GL13.GL_TEXTURE_CUBE_MAP;
import static org.lwjgl.opengl.GL13.GL_TEXTURE_CUBE_MAP_POSITIVE_X;
import static org.lwjgl.opengl.GL14.GL_GENERATE_MIPMAP;
import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glUniform1i;
import static org.lwjgl.opengl.GL20.glUniformMatrix4fv;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL43C.*;
import static org.lwjgl.stb.STBImage.*;
import static org.lwjgl.system.MemoryUtil.*;
import java.lang.Math;

/**
 * Shows how to load models in Wavefront obj and mlt format with Assimp binding and render them with
 * OpenGL.
 *
 * @author Zhang Hai
 */
public class WavefrontObjDemo {

    long window;
    int width = 1024;
    int height = 768;
    int fbWidth = 1024;
    int fbHeight = 768;
    float fov = 60;
    float rotation;

    int program;
    int programcube;

    private int pttex;
    private int sampler;

    int vertexAttribute;
    int normalAttribute;
    int modelMatrixUniform;
    int viewProjectionMatrixUniform;
    int normalMatrixUniform;
    int lightPositionUniform;
    int viewPositionUniform;
    int ambientColorUniform;
    int diffuseColorUniform;
    int specularColorUniform;

    private int vao;
    private int quadProgram;

    public int cubemapProgram;
    public int cubemap_invViewProjUniform;

    public FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);
    private Matrix4f invViewProjMatrix = new Matrix4f();
    public ByteBuffer quadVertices;

    Model model;

    CreatesMainTest creates = new CreatesMainTest(this);

    Matrix4f modelMatrix = new Matrix4f().rotateY(0.5f * (float) Math.PI).scale(1.5f, 1.5f, 1.5f);
    Matrix4f viewMatrix = new Matrix4f();
    Matrix4f projectionMatrix = new Matrix4f();
    Matrix4f viewProjectionMatrix = new Matrix4f();
    Vector3f viewPosition = new Vector3f();
    Vector3f lightPosition = new Vector3f(-5f, 5f, 5f);

    private FloatBuffer modelMatrixBuffer = BufferUtils.createFloatBuffer(4 * 4);
    private FloatBuffer viewProjectionMatrixBuffer = BufferUtils.createFloatBuffer(4 * 4);
    private Matrix3f normalMatrix = new Matrix3f();
    private FloatBuffer normalMatrixBuffer = BufferUtils.createFloatBuffer(3 * 3);
    private FloatBuffer lightPositionBuffer = BufferUtils.createFloatBuffer(3);
    private FloatBuffer viewPositionBuffer = BufferUtils.createFloatBuffer(3);

    public GLCapabilities caps;
    GLFWKeyCallback keyCallback;
    GLFWFramebufferSizeCallback fbCallback;
    GLFWWindowSizeCallback wsCallback;
    GLFWCursorPosCallback cpCallback;
    GLFWScrollCallback sCallback;
    Callback debugProc;

    void init() throws IOException {

        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        window = glfwCreateWindow(width, height,
                "Wavefront obj model loading with Assimp demo", NULL, NULL);
        if (window == NULL)
            throw new AssertionError("Failed to create the GLFW window");

        System.out.println("Move the mouse to look around");
        System.out.println("Zoom in/out with mouse wheel");
        glfwSetFramebufferSizeCallback(window, fbCallback = new GLFWFramebufferSizeCallback() {
            @Override
            public void invoke(long window, int width, int height) {
                if (width > 0 && height > 0 && (WavefrontObjDemo.this.fbWidth != width
                        || WavefrontObjDemo.this.fbHeight != height)) {
                    WavefrontObjDemo.this.fbWidth = width;
                    WavefrontObjDemo.this.fbHeight = height;
                }
            }
        });
        glfwSetWindowSizeCallback(window, wsCallback = new GLFWWindowSizeCallback() {
            @Override
            public void invoke(long window, int width, int height) {
                if (width > 0 && height > 0 && (WavefrontObjDemo.this.width != width
                        || WavefrontObjDemo.this.height != height)) {
                    WavefrontObjDemo.this.width = width;
                    WavefrontObjDemo.this.height = height;
                }
            }
        });
        glfwSetKeyCallback(window, keyCallback = new GLFWKeyCallback() {
            @Override
            public void invoke(long window, int key, int scancode, int action, int mods) {
                if (action != GLFW_RELEASE) {
                    return;
                }
                if (key == GLFW_KEY_ESCAPE) {
                    glfwSetWindowShouldClose(window, true);
                }
            }
        });
        glfwSetCursorPosCallback(window, cpCallback = new GLFWCursorPosCallback() {
            @Override
            public void invoke(long window, double x, double y) {
                rotation = ((float) x / width - 0.5f) * 2f * (float) Math.PI;
            }
        });
        glfwSetScrollCallback(window, sCallback = new GLFWScrollCallback() {
            @Override
            public void invoke(long window, double xoffset, double yoffset) {
                if (yoffset < 0) {
                    fov *= 1.05f;
                } else {
                    fov *= 1f / 1.05f;
                }
                if (fov < 10.0f) {
                    fov = 10.0f;
                } else if (fov > 120.0f) {
                    fov = 120.0f;
                }
            }
        });

        GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        glfwSetWindowPos(window, (vidmode.width() - width) / 2, (vidmode.height() - height) / 2);
        glfwMakeContextCurrent(window);
        glfwSwapInterval(0);
        glfwSetCursorPos(window, width / 2, height / 2);

        try (MemoryStack frame = MemoryStack.stackPush()) {
            IntBuffer framebufferSize = frame.mallocInt(2);
            nglfwGetFramebufferSize(window, memAddress(framebufferSize), memAddress(framebufferSize) + 4);
            width = framebufferSize.get(0);
            height = framebufferSize.get(1);
        }

        IntBuffer framebufferSize = BufferUtils.createIntBuffer(2);
        nglfwGetFramebufferSize(window, memAddress(framebufferSize), memAddress(framebufferSize) + 4);
        caps = GL.createCapabilities();
        debugProc = GLUtil.setupDebugMessageCallback();

        //glClearColor(0f, 0f, 0f, 1f);
        //glEnable(GL_DEPTH_TEST);

        /* Create all needed GL resources */
        //loadModel();
        //createProgram();

       //createProgram2();
        /* Show window */
        /*creates.createCubemapTexture();
        creates.createFullScreenQuad();
        creates.createCubemapProgram();*/


        glfwMakeContextCurrent(window);
        glfwSwapInterval(0);
        glfwShowWindow(window);


        createCubemapTexture();
        createFullScreenQuad();
        createCubemapProgram();

        createProgramModel();

        loadModel();

        glEnableClientState(GL_VERTEX_ARRAY);
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE);
    }

    void loadModel() {
        AIFileIO fileIo = AIFileIO.create();
        AIFileOpenProcI fileOpenProc = new AIFileOpenProc() {
            public long invoke(long pFileIO, long fileName, long openMode) {
                AIFile aiFile = AIFile.create();
                final ByteBuffer data;
                String fileNameUtf8 = memUTF8(fileName);
                try {
                    data = ioResourceToByteBuffer(fileNameUtf8, 8192);
                } catch (IOException e) {
                    throw new RuntimeException("Could not open file: " + fileNameUtf8);
                }
                AIFileReadProcI fileReadProc = new AIFileReadProc() {
                    public long invoke(long pFile, long pBuffer, long size, long count) {
                        long max = Math.min(data.remaining(), size * count);
                        memCopy(memAddress(data) + data.position(), pBuffer, max);
                        return max;
                    }
                };
                AIFileSeekI fileSeekProc = new AIFileSeek() {
                    public int invoke(long pFile, long offset, int origin) {
                        if (origin == Assimp.aiOrigin_CUR) {
                            data.position(data.position() + (int) offset);
                        } else if (origin == Assimp.aiOrigin_SET) {
                            data.position((int) offset);
                        } else if (origin == Assimp.aiOrigin_END) {
                            data.position(data.limit() + (int) offset);
                        }
                        return 0;
                    }
                };
                AIFileTellProcI fileTellProc = new AIFileTellProc() {
                    public long invoke(long pFile) {
                        return data.limit();
                    }
                };
                aiFile.ReadProc(fileReadProc);
                aiFile.SeekProc(fileSeekProc);
                aiFile.FileSizeProc(fileTellProc);
                return aiFile.address();
            }
        };
        AIFileCloseProcI fileCloseProc = new AIFileCloseProc() {
            public void invoke(long pFileIO, long pFile) {
                /* Nothing to do */
            }
        };
        fileIo.set(fileOpenProc, fileCloseProc, NULL);
        AIScene scene = aiImportFileEx("org/lwjgl/demo/opengl/assimp/earth3.obj",
                aiProcess_JoinIdenticalVertices | aiProcess_Triangulate, fileIo);
        if (scene == null) {
            throw new IllegalStateException(aiGetErrorString());
        }
        model = new Model(scene);
    }

    public static int createProgram(int vshader, int fshader) {
        int program = glCreateProgramObjectARB();
        glAttachObjectARB(program, vshader);
        glAttachObjectARB(program, fshader);
        glLinkProgramARB(program);
        int linkStatus = glGetObjectParameteriARB(program, GL_OBJECT_LINK_STATUS_ARB);
        String programLog = glGetInfoLogARB(program);
        if (programLog.trim().length() > 0) {
            System.err.println(programLog);
        }
        if (linkStatus == 0) {
            throw new AssertionError("Could not link program");
        }
        return program;
    }

    static int createShader(String resource, int type) throws IOException {
        int shader = glCreateShaderObjectARB(type);
        ByteBuffer source = ioResourceToByteBuffer(resource, 1024);
        PointerBuffer strings = BufferUtils.createPointerBuffer(1);
        IntBuffer lengths = BufferUtils.createIntBuffer(1);
        strings.put(0, source);
        lengths.put(0, source.remaining());
        glShaderSourceARB(shader, strings, lengths);
        glCompileShaderARB(shader);
        int compiled = glGetObjectParameteriARB(shader, GL_OBJECT_COMPILE_STATUS_ARB);
        String shaderLog = glGetInfoLogARB(shader);
        if (shaderLog.trim().length() > 0) {
            System.err.println(shaderLog);
        }
        if (compiled == 0) {
            throw new AssertionError("Could not compile shader");
        }
        return shader;
    }

    /*void createProgram() throws IOException {

        program = glCreateProgramObjectARB();
        int vertexShader = createShader("org/lwjgl/demo/opengl/assimp/magnet.vs",
                GL_VERTEX_SHADER_ARB);
        int fragmentShader = createShader("org/lwjgl/demo/opengl/assimp/magnet.fs",
                GL_FRAGMENT_SHADER_ARB);
        glAttachObjectARB(program, vertexShader);
        glAttachObjectARB(program, fragmentShader);
        glLinkProgramARB(program);
        int linkStatus = glGetObjectParameteriARB(program, GL_OBJECT_LINK_STATUS_ARB);
        String programLog = glGetInfoLogARB(program);
        if (programLog.trim().length() > 0) {
            System.err.println(programLog);
        }
        if (linkStatus == 0) {
            throw new AssertionError("Could not link program");
        }

        glUseProgramObjectARB(program);
        vertexAttribute = glGetAttribLocationARB(program, "aVertex");
        glEnableVertexAttribArrayARB(vertexAttribute);
        normalAttribute = glGetAttribLocationARB(program, "aNormal");
        glEnableVertexAttribArrayARB(normalAttribute);

        modelMatrixUniform = glGetUniformLocationARB(program, "uModelMatrix");
        viewProjectionMatrixUniform = glGetUniformLocationARB(program, "uViewProjectionMatrix");
        normalMatrixUniform = glGetUniformLocationARB(program, "uNormalMatrix");
        lightPositionUniform = glGetUniformLocationARB(program, "uLightPosition");
        viewPositionUniform = glGetUniformLocationARB(program, "uViewPosition");
        ambientColorUniform = glGetUniformLocationARB(program, "uAmbientColor");
        diffuseColorUniform = glGetUniformLocationARB(program, "uDiffuseColor");
        specularColorUniform = glGetUniformLocationARB(program, "uSpecularColor");
    }*/

    void createProgramModel() throws IOException {

        int vertexShader = createShader("org/lwjgl/demo/opengl/assimp/magnet.vs", GL_VERTEX_SHADER_ARB);
        int fragmentShader = createShader("org/lwjgl/demo/opengl/assimp/magnet.fs", GL_FRAGMENT_SHADER_ARB);
        int program = createProgram(vertexShader, fragmentShader);

        glUseProgramObjectARB(cubemapProgram);

        vertexAttribute = glGetAttribLocationARB(program, "aVertex");
        glEnableVertexAttribArrayARB(vertexAttribute);
        normalAttribute = glGetAttribLocationARB(program, "aNormal");
        glEnableVertexAttribArrayARB(normalAttribute);

        modelMatrixUniform = glGetUniformLocationARB(program, "uModelMatrix");
        viewProjectionMatrixUniform = glGetUniformLocationARB(program, "uViewProjectionMatrix");
        normalMatrixUniform = glGetUniformLocationARB(program, "uNormalMatrix");
        lightPositionUniform = glGetUniformLocationARB(program, "uLightPosition");
        viewPositionUniform = glGetUniformLocationARB(program, "uViewPosition");
        ambientColorUniform = glGetUniformLocationARB(program, "uAmbientColor");
        diffuseColorUniform = glGetUniformLocationARB(program, "uDiffuseColor");
        specularColorUniform = glGetUniformLocationARB(program, "uSpecularColor");

        this.program = program;
    }

    public void createCubemapProgram() throws IOException {
        int vshader = createShader("org/lwjgl/demo/game/cubemap.vs", GL_VERTEX_SHADER);
        int fshader = createShader("org/lwjgl/demo/game/cubemap.fs", GL_FRAGMENT_SHADER);
        int program = createProgram(vshader, fshader);

        glUseProgramObjectARB(program);

        int texLocation = glGetUniformLocationARB(program, "tex");
        glUniform1i(texLocation, 0);
        this.cubemap_invViewProjUniform = glGetUniformLocationARB(program, "invViewProj");

        glUseProgramObjectARB(0);

        this.cubemapProgram = program;
    }

    public void createFullScreenQuad() {
        this.quadVertices = BufferUtils.createByteBuffer(4 * 2 * 6);
        FloatBuffer fv = this.quadVertices.asFloatBuffer();
        fv.put(-1.0f).put(-1.0f);
        fv.put( 1.0f).put(-1.0f);
        fv.put( 1.0f).put( 1.0f);
        fv.put( 1.0f).put( 1.0f);
        fv.put(-1.0f).put( 1.0f);
        fv.put(-1.0f).put(-1.0f);
    }

    public void createCubemapTexture() throws IOException {
        int tex = glGenTextures();
        glBindTexture(GL_TEXTURE_CUBE_MAP, tex);
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        ByteBuffer imageBuffer;
        IntBuffer w = BufferUtils.createIntBuffer(1);
        IntBuffer h = BufferUtils.createIntBuffer(1);
        IntBuffer comp = BufferUtils.createIntBuffer(1);
        String[] names = { "right", "left", "top", "bottom", "front", "back" };
        ByteBuffer image;
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_GENERATE_MIPMAP, GL_TRUE);
        for (int i = 0; i < 6; i++) {
            imageBuffer = ioResourceToByteBuffer("org/lwjgl/demo/space_" + names[i] + (i + 1) + ".jpg", 8 * 1024);
            if (!stbi_info_from_memory(imageBuffer, w, h, comp))
                throw new IOException("Failed to read image information: " + stbi_failure_reason());
            image = stbi_load_from_memory(imageBuffer, w, h, comp, 0);
            if (image == null)
                throw new IOException("Failed to load image: " + stbi_failure_reason());
            glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, 0, GL_RGB8, w.get(0), h.get(0), 0, GL_RGB, GL_UNSIGNED_BYTE, image);
            stbi_image_free(image);
        }
        if (this.caps.OpenGL32 || this.caps.GL_ARB_seamless_cube_map) {
            glEnable(GL_TEXTURE_CUBE_MAP_SEAMLESS);
        }
    }

    void update() {
        projectionMatrix.setPerspective((float) Math.toRadians(fov), (float) width / height, 0.01f,
                100.0f);
        viewPosition.set(10f * (float) Math.cos(rotation), 10f, 10f * (float) Math.sin(rotation));
        viewMatrix.setLookAt(viewPosition.x, viewPosition.y, viewPosition.z, 0f, 0f, 0f, 0f, 1f,
                0f);
        projectionMatrix.mul(viewMatrix, viewProjectionMatrix);

        glUseProgramObjectARB(cubemapProgram);
        glUniformMatrix4fv(cubemap_invViewProjUniform, false, invViewProjMatrix.get(matrixBuffer));
    }

    private void drawCubemap() {
        glUseProgramObjectARB(cubemapProgram); // cubemapProgram
        glVertexPointer(2, GL_FLOAT, 0, quadVertices);
        glDrawArrays(GL_TRIANGLES, 0, 6);
    }

    void render() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        drawCubemap();

        glUseProgram(program);

        for (Model.Mesh mesh : model.meshes) {

            glBindBuffer(GL_ARRAY_BUFFER, mesh.vertexArrayBuffer);
            glVertexAttribPointer(vertexAttribute, 3, GL_FLOAT, false, 0, 0);
            glBindBuffer(GL_ARRAY_BUFFER, 0);

            glBindBuffer(GL_ARRAY_BUFFER, mesh.normalArrayBuffer);
            glVertexAttribPointer(normalAttribute, 3, GL_FLOAT, false, 0, 0);
            glBindBuffer(GL_ARRAY_BUFFER, 0);

            glUniformMatrix4fv(modelMatrixUniform, false, modelMatrix.get(modelMatrixBuffer));
            glUniformMatrix4fv(viewProjectionMatrixUniform, false,
                    viewProjectionMatrix.get(viewProjectionMatrixBuffer));
            normalMatrix.set(modelMatrix).invert().transpose();
            glUniformMatrix3fv(normalMatrixUniform, false, normalMatrix.get(normalMatrixBuffer));
            glUniform3fv(lightPositionUniform, lightPosition.get(lightPositionBuffer));
            glUniform3fv(viewPositionUniform, viewPosition.get(viewPositionBuffer));

            Model.Material material = model.materials.get(mesh.mesh.mMaterialIndex());
            nglUniform3fv(ambientColorUniform, 1, material.mAmbientColor.address());
            nglUniform3fv(glGetUniformLocation(program, "uDiffuseColor"), 1, material.mDiffuseColor.address());
            nglUniform3fv(specularColorUniform, 1, material.mSpecularColor.address());

            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, mesh.elementArrayBuffer);
            glDrawElements(GL_TRIANGLES, mesh.elementCount, GL_UNSIGNED_INT, 0);
        }
    }

    void loop() {
        //glClearColor(1.0f, 1.0f, 1.0f, 0.0f);

        while (!glfwWindowShouldClose(window)) {
            glfwPollEvents();
            glViewport(0, 0, fbWidth, fbHeight);
            update();
            render();
            glfwSwapBuffers(window);
        }
    }

    void run() {
        try {
            init();
            loop();
            //model.free();
            if (debugProc != null) {
                debugProc.free();
            }
            cpCallback.free();
            keyCallback.free();
            fbCallback.free();
            wsCallback.free();
            glfwDestroyWindow(window);
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            glfwTerminate();
        }
    }

    public static void main(String[] args) {
        new WavefrontObjDemo().run();
    }

    static class Model {

        public AIScene scene;
        public List<Mesh> meshes;
        public List<Material> materials;

        public Model(AIScene scene) {

            this.scene = scene;

            int meshCount = scene.mNumMeshes();
            PointerBuffer meshesBuffer = scene.mMeshes();
            meshes = new ArrayList<>();
            for (int i = 0; i < meshCount; ++i) {
                meshes.add(new Mesh(AIMesh.create(meshesBuffer.get(i))));
            }

            int materialCount = scene.mNumMaterials();
            PointerBuffer materialsBuffer = scene.mMaterials();
            materials = new ArrayList<>();
            for (int i = 0; i < materialCount; ++i) {
                materials.add(new Material(AIMaterial.create(materialsBuffer.get(i))));
            }
        }

        public void free() {
            aiReleaseImport(scene);
            scene = null;
            meshes = null;
            materials = null;
        }

        public static class Mesh {

            public AIMesh mesh;
            public int vertexArrayBuffer;
            public int normalArrayBuffer;
            public int elementArrayBuffer;
            public int elementCount;

            public Mesh(AIMesh mesh) {
                this.mesh = mesh;

                vertexArrayBuffer = glGenBuffersARB();
                glBindBufferARB(GL_ARRAY_BUFFER_ARB, vertexArrayBuffer);
                AIVector3D.Buffer vertices = mesh.mVertices();
                nglBufferDataARB(GL_ARRAY_BUFFER_ARB, AIVector3D.SIZEOF * vertices.remaining(),
                        vertices.address(), GL_STATIC_DRAW_ARB);
                glBindBufferARB(GL_ARRAY_BUFFER_ARB, 0);

                normalArrayBuffer = glGenBuffersARB();
                glBindBufferARB(GL_ARRAY_BUFFER_ARB, normalArrayBuffer);
                AIVector3D.Buffer normals = mesh.mNormals();
                nglBufferDataARB(GL_ARRAY_BUFFER_ARB, AIVector3D.SIZEOF * normals.remaining(),
                        normals.address(), GL_STATIC_DRAW_ARB);
                glBindBufferARB(GL_ARRAY_BUFFER_ARB, 0);

                int faceCount = mesh.mNumFaces();
                elementCount = faceCount * 3;
                IntBuffer elementArrayBufferData = BufferUtils.createIntBuffer(elementCount);
                AIFace.Buffer facesBuffer = mesh.mFaces();
                for (int i = 0; i < faceCount; ++i) {
                    AIFace face = facesBuffer.get(i);
                    if (face.mNumIndices() != 3) {
                        throw new IllegalStateException("AIFace.mNumIndices() != 3");
                    }
                    elementArrayBufferData.put(face.mIndices());
                }
                elementArrayBufferData.flip();
                elementArrayBuffer = glGenBuffersARB();
                glBindBufferARB(GL_ELEMENT_ARRAY_BUFFER_ARB, elementArrayBuffer);
                glBufferDataARB(GL_ELEMENT_ARRAY_BUFFER_ARB, elementArrayBufferData,
                        GL_STATIC_DRAW_ARB);
                glBindBufferARB(GL_ARRAY_BUFFER_ARB, 0);
            }
        }

        public static class Material {

            public AIMaterial mMaterial;
            public AIColor4D mAmbientColor;
            public AIColor4D mDiffuseColor;
            public AIColor4D mSpecularColor;

            public Material(AIMaterial material) {

                mMaterial = material;

                mAmbientColor = AIColor4D.create();
                if (aiGetMaterialColor(mMaterial, AI_MATKEY_COLOR_AMBIENT,
                        aiTextureType_NONE, 0, mAmbientColor) != 0) {
                    throw new IllegalStateException(aiGetErrorString());
                }
                mDiffuseColor = AIColor4D.create();
                if (aiGetMaterialColor(mMaterial, AI_MATKEY_COLOR_DIFFUSE,
                        aiTextureType_NONE, 0, mDiffuseColor) != 0) {
                    throw new IllegalStateException(aiGetErrorString());
                }
                mSpecularColor = AIColor4D.create();
                if (aiGetMaterialColor(mMaterial, AI_MATKEY_COLOR_SPECULAR,
                        aiTextureType_NONE, 0, mSpecularColor) != 0) {
                    throw new IllegalStateException(aiGetErrorString());
                }
            }
        }
    }
}