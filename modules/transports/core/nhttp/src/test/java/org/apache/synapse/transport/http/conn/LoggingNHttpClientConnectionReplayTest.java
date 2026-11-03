/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com) All Rights Reserved.
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.transport.http.conn;

import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponseFactory;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.nio.reactor.IOSession;
import org.apache.http.nio.util.ByteBufferAllocator;
import org.apache.http.nio.util.HeapByteBufferAllocator;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.synapse.transport.passthru.PassThroughConstants;
import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Tests which I/O session {@link LoggingNHttpClientConnection} binds to, with the replay transaction
 * feature disabled and enabled.
 * <p>
 * The replay flag is a static final value read from passthru-http.properties when the class is
 * loaded. The enabled case therefore loads a fresh copy of the connection classes in a separate class
 * loader that sees a passthru-http.properties with the feature turned on.
 */
public class LoggingNHttpClientConnectionReplayTest {

    private static final String CONN_PACKAGE = "org.apache.synapse.transport.http.conn.";

    private boolean debuggerWasEnabled;

    @Before
    public void setUp() {
        debuggerWasEnabled = SynapseDebugInfoHolder.getInstance().isDebuggerEnabled();
    }

    @After
    public void tearDown() {
        SynapseDebugInfoHolder.getInstance().setDebuggerEnabled(debuggerWasEnabled);
    }

    private static IOSession newSession() {
        IOSession session = Mockito.mock(IOSession.class);
        Mockito.when(session.getStatus()).thenReturn(IOSession.ACTIVE);
        return session;
    }

    private static Object boundSession(Object connection) throws Exception {
        Class<?> type = connection.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField("session");
                field.setAccessible(true);
                return field.get(connection);
            } catch (NoSuchFieldException e) {
                type = type.getSuperclass();
            }
        }
        throw new AssertionError("No session field found");
    }

    private static Object wrappedSession(Object wrapper) throws Exception {
        Field field = wrapper.getClass().getDeclaredField("session");
        field.setAccessible(true);
        return field.get(wrapper);
    }

    private static boolean replayFlag(Class<?> connectionClass) throws Exception {
        Field field = connectionClass.getDeclaredField("REPLAY_TXN_ENABLED");
        field.setAccessible(true);
        return field.getBoolean(null);
    }

    private static LoggingNHttpClientConnection newConnection(IOSession session) {
        return new LoggingNHttpClientConnection(session, DefaultHttpResponseFactory.INSTANCE,
                HeapByteBufferAllocator.INSTANCE, new BasicHttpParams());
    }

    private static boolean ioOrWireDebugEnabled() {
        return LogFactory.getLog(newSession().getClass()).isDebugEnabled()
                || LogFactory.getLog(LoggingUtils.WIRE_LOG_ID).isDebugEnabled();
    }

    // ---- Replay disabled (the default) ----

    @Test
    public void testRebindKeepsWireLoggingWhenReplayIsDisabled() throws Exception {
        Assume.assumeFalse(replayFlag(LoggingNHttpClientConnection.class));
        SynapseDebugInfoHolder.getInstance().setDebuggerEnabled(true);
        LoggingNHttpClientConnection connection = newConnection(newSession());
        IOSession upgraded = newSession();

        connection.bind(upgraded);

        Object bound = boundSession(connection);
        Assert.assertTrue("Wire logging must survive a re-bind (e.g. TLS upgrade through a proxy tunnel) "
                + "but the connection was bound to " + bound.getClass().getName(), bound instanceof LoggingIOSession);
        Assert.assertSame(upgraded, wrappedSession(bound));
        Assert.assertSame(upgraded, connection.getIOSession());
    }

    @Test
    public void testConstructorUsesWireLoggingWhenReplayIsDisabled() throws Exception {
        Assume.assumeFalse(replayFlag(LoggingNHttpClientConnection.class));
        SynapseDebugInfoHolder.getInstance().setDebuggerEnabled(true);
        IOSession session = newSession();

        LoggingNHttpClientConnection connection = newConnection(session);

        Assert.assertTrue(boundSession(connection) instanceof LoggingIOSession);
        Assert.assertSame(session, connection.getIOSession());
    }

    @Test
    public void testRebindUsesPlainSessionWhenNothingIsEnabled() throws Exception {
        Assume.assumeFalse(replayFlag(LoggingNHttpClientConnection.class));
        String ioLogName = newSession().getClass().getName();
        Object ioLevel = setLog4jLevel(ioLogName, "INFO");
        Object wireLevel = setLog4jLevel(LoggingUtils.WIRE_LOG_ID, "INFO");
        try {
            Assume.assumeFalse("I/O or wire debug logging could not be turned off", ioOrWireDebugEnabled());
            SynapseDebugInfoHolder.getInstance().setDebuggerEnabled(false);
            LoggingNHttpClientConnection connection = newConnection(newSession());
            IOSession upgraded = newSession();

            connection.bind(upgraded);

            Assert.assertSame(upgraded, boundSession(connection));
            Assert.assertSame(upgraded, connection.getIOSession());
        } finally {
            restoreLog4jLevel(ioLogName, ioLevel);
            restoreLog4jLevel(LoggingUtils.WIRE_LOG_ID, wireLevel);
        }
    }

    /**
     * Sets a log4j 1.x logger level if log4j 1.x backs commons-logging in the test class path (an
     * unconfigured log4j 1.x logs everything at DEBUG). Returns the previous level, or null.
     */
    private static Object setLog4jLevel(String loggerName, String level) {
        try {
            Class<?> loggerClass = Class.forName("org.apache.log4j.Logger");
            Class<?> levelClass = Class.forName("org.apache.log4j.Level");
            Object logger = loggerClass.getMethod("getLogger", String.class).invoke(null, loggerName);
            Object previous = loggerClass.getMethod("getLevel").invoke(logger);
            Object newLevel = levelClass.getMethod("toLevel", String.class).invoke(null, level);
            loggerClass.getMethod("setLevel", levelClass).invoke(logger, newLevel);
            return previous;
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private static void restoreLog4jLevel(String loggerName, Object previous) {
        try {
            Class<?> loggerClass = Class.forName("org.apache.log4j.Logger");
            Class<?> levelClass = Class.forName("org.apache.log4j.Level");
            Object logger = loggerClass.getMethod("getLogger", String.class).invoke(null, loggerName);
            loggerClass.getMethod("setLevel", levelClass).invoke(logger, previous);
        } catch (ReflectiveOperationException e) {
            // log4j 1.x not present; nothing to restore
        }
    }

    // ---- Replay enabled ----

    /**
     * Loads classes of the http.conn package from the main classes directory before asking the parent,
     * so that they are initialised again with a different passthru-http.properties.
     */
    private static class ChildFirstConnClassLoader extends URLClassLoader {

        ChildFirstConnClassLoader(URL classes, ClassLoader parent) {
            super(new URL[]{classes}, parent);
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (!name.startsWith(CONN_PACKAGE)) {
                return super.loadClass(name, resolve);
            }
            synchronized (getClassLoadingLock(name)) {
                Class<?> type = findLoadedClass(name);
                if (type == null) {
                    try {
                        type = findClass(name);
                    } catch (ClassNotFoundException e) {
                        type = super.loadClass(name, false);
                    }
                }
                if (resolve) {
                    resolveClass(type);
                }
                return type;
            }
        }
    }

    private static class ReplayEnabledFixture implements AutoCloseable {

        final Class<?> connectionClass;
        final Object debugInfoHolder;
        private final File confDir;
        private final ChildFirstConnClassLoader connLoader;

        ReplayEnabledFixture() throws Exception {
            confDir = Files.createTempDirectory("replay-conf").toFile();
            File propertiesFile = new File(confDir, "passthru-http.properties");
            try (OutputStream out = new FileOutputStream(propertiesFile)) {
                out.write("pass.through.transport.enableReplayTransaction=true\n".getBytes(StandardCharsets.UTF_8));
            }
            URL classes = LoggingNHttpClientConnection.class.getProtectionDomain().getCodeSource().getLocation();
            connLoader = new ChildFirstConnClassLoader(classes, getClass().getClassLoader());
            // passthru-http.properties is looked up in the conf.location directory first. Other tests in this
            // module set that property, so point it at the temporary directory while the class is initialised.
            String previousConfLocation = System.getProperty(PassThroughConstants.CONF_LOCATION);
            System.setProperty(PassThroughConstants.CONF_LOCATION, confDir.getAbsolutePath());
            try {
                connectionClass = Class.forName(CONN_PACKAGE + "LoggingNHttpClientConnection", true, connLoader);
            } finally {
                if (previousConfLocation == null) {
                    System.clearProperty(PassThroughConstants.CONF_LOCATION);
                } else {
                    System.setProperty(PassThroughConstants.CONF_LOCATION, previousConfLocation);
                }
            }
            Assert.assertNotSame("Connection class must come from the isolated loader",
                    LoggingNHttpClientConnection.class, connectionClass);
            Class<?> holderClass = Class.forName(CONN_PACKAGE + "SynapseDebugInfoHolder", true, connLoader);
            debugInfoHolder = holderClass.getMethod("getInstance").invoke(null);
        }

        void setDebuggerEnabled(boolean enabled) throws Exception {
            debugInfoHolder.getClass().getMethod("setDebuggerEnabled", boolean.class).invoke(debugInfoHolder, enabled);
        }

        Object newConnection(IOSession session) throws Exception {
            Constructor<?> constructor = connectionClass.getConstructor(IOSession.class, HttpResponseFactory.class,
                    ByteBufferAllocator.class, HttpParams.class);
            return constructor.newInstance(session, DefaultHttpResponseFactory.INSTANCE,
                    HeapByteBufferAllocator.INSTANCE, new BasicHttpParams());
        }

        void bind(Object connection, IOSession session) throws Exception {
            Method bind = connectionClass.getMethod("bind", IOSession.class);
            bind.invoke(connection, session);
        }

        Object ioSession(Object connection) throws Exception {
            return connectionClass.getMethod("getIOSession").invoke(connection);
        }

        @Override
        public void close() throws Exception {
            connLoader.close();
            new File(confDir, "passthru-http.properties").delete();
            confDir.delete();
        }
    }

    /**
     * Asserts that the bound session is a replay session around the given session, with a wire-log
     * session in between when wire logging is expected.
     */
    private static void assertReplaySession(Object bound, IOSession expectedDelegate, boolean wireLogging)
            throws Exception {
        Assert.assertEquals(CONN_PACKAGE + "ReplayIOSession", bound.getClass().getName());
        Object inner = wrappedSession(bound);
        if (wireLogging) {
            Assert.assertEquals("Replay must wrap the wire-log session, not replace it",
                    CONN_PACKAGE + "LoggingIOSession", inner.getClass().getName());
            Assert.assertSame(expectedDelegate, wrappedSession(inner));
        } else {
            Assert.assertSame(expectedDelegate, inner);
        }
    }

    /**
     * Runs the check with the debugger enabled (wire logging on) and with debugger, I/O and wire logs off.
     */
    private static void forWireLoggingOnAndOff(ReplayEnabledFixture fixture, WireLoggingCheck check)
            throws Exception {
        fixture.setDebuggerEnabled(true);
        check.run(true);

        String ioLogName = newSession().getClass().getName();
        Object ioLevel = setLog4jLevel(ioLogName, "INFO");
        Object wireLevel = setLog4jLevel(LoggingUtils.WIRE_LOG_ID, "INFO");
        try {
            Assume.assumeFalse("I/O or wire debug logging could not be turned off", ioOrWireDebugEnabled());
            fixture.setDebuggerEnabled(false);
            check.run(false);
        } finally {
            restoreLog4jLevel(ioLogName, ioLevel);
            restoreLog4jLevel(LoggingUtils.WIRE_LOG_ID, wireLevel);
        }
    }

    private interface WireLoggingCheck {
        void run(boolean wireLogging) throws Exception;
    }

    @Test
    public void testConstructorUsesReplaySessionWhenReplayIsEnabled() throws Exception {
        try (ReplayEnabledFixture fixture = new ReplayEnabledFixture()) {
            Assert.assertTrue("Replay flag must be read from passthru-http.properties",
                    replayFlag(fixture.connectionClass));
            forWireLoggingOnAndOff(fixture, wireLogging -> {
                IOSession session = newSession();

                Object connection = fixture.newConnection(session);

                assertReplaySession(boundSession(connection), session, wireLogging);
                Assert.assertSame(session, fixture.ioSession(connection));
            });
        }
    }

    @Test
    public void testRebindUsesReplaySessionWhenReplayIsEnabled() throws Exception {
        try (ReplayEnabledFixture fixture = new ReplayEnabledFixture()) {
            forWireLoggingOnAndOff(fixture, wireLogging -> {
                Object connection = fixture.newConnection(newSession());
                IOSession upgraded = newSession();

                fixture.bind(connection, upgraded);

                assertReplaySession(boundSession(connection), upgraded, wireLogging);
                Assert.assertSame(upgraded, fixture.ioSession(connection));
            });
        }
    }

    @Test
    public void testWritesPassThroughWireLogSessionWhenReplayIsEnabled() throws Exception {
        try (ReplayEnabledFixture fixture = new ReplayEnabledFixture()) {
            fixture.setDebuggerEnabled(true);
            IOSession session = newSession();
            ByteChannel channel = Mockito.mock(ByteChannel.class);
            Mockito.when(session.channel()).thenReturn(channel);
            Mockito.when(channel.write(Mockito.any(ByteBuffer.class))).thenAnswer(invocation -> {
                ByteBuffer src = invocation.getArgument(0);
                int remaining = src.remaining();
                src.position(src.limit());
                return remaining;
            });
            Object connection = fixture.newConnection(session);
            IOSession bound = (IOSession) boundSession(connection);

            int written = bound.channel().write(ByteBuffer.wrap("POST / HTTP/1.1\r\n".getBytes(StandardCharsets.US_ASCII)));

            Assert.assertEquals(17, written);
            Mockito.verify(channel, Mockito.times(1)).write(Mockito.any(ByteBuffer.class));
            Assert.assertEquals(CONN_PACKAGE + "LoggingIOSession", wrappedSession(bound).getClass().getName());
        }
    }

    @Test
    public void testReplayFlagIsDisabledByDefault() throws Exception {
        Assert.assertFalse("Replay must be off unless enabled in passthru-http.properties",
                replayFlag(LoggingNHttpClientConnection.class));
    }
}
