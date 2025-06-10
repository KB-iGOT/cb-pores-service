package com.igot.cb.pores.logger;

import org.apache.htrace.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CbExtLoggerTest {

    private CbExtLogger cbExtLogger;
    private Logger mockLogger;

    @BeforeEach
    void setUp() throws Exception {
        cbExtLogger = new CbExtLogger("TestClass");

        mockLogger = Mockito.mock(Logger.class);

        when(mockLogger.isDebugEnabled()).thenReturn(true);
        when(mockLogger.isInfoEnabled()).thenReturn(true);
        when(mockLogger.isTraceEnabled()).thenReturn(true);

        Field loggerField = CbExtLogger.class.getDeclaredField("logger");
        loggerField.setAccessible(true);
        loggerField.set(cbExtLogger, mockLogger);

        // Recalculate flags
        Field debugField = CbExtLogger.class.getDeclaredField("isDebugEnabled");
        debugField.setAccessible(true);
        debugField.set(cbExtLogger, true);

        Field infoField = CbExtLogger.class.getDeclaredField("isInfoEnabled");
        infoField.setAccessible(true);
        infoField.set(cbExtLogger, true);

        Field traceField = CbExtLogger.class.getDeclaredField("isTraceEnabled");
        traceField.setAccessible(true);
        traceField.set(cbExtLogger, true);
    }

    @Test
    void testDebug() {
        cbExtLogger.debug("debug log");
        verify(mockLogger).log(Level.DEBUG, "debug log");
    }

    @Test
    void testInfo() {
        cbExtLogger.info("info log");
        verify(mockLogger).log(Level.INFO, "info log");
    }

    @Test
    void testWarn() {
        cbExtLogger.warn("warn log");
        verify(mockLogger).log(Level.WARN, "warn log");
    }

    @Test
    void testErrorWithException() {
        Exception ex = new RuntimeException("Error");
        cbExtLogger.error(ex);
        verify(mockLogger).log(eq(Level.ERROR), contains("Error"));
    }

    @Test
    void testErrorWithMessageAndException() {
        Exception ex = new RuntimeException("Error msg");
        cbExtLogger.error("custom error", ex);
        verify(mockLogger).error("custom error", ex);
    }

    @Test
    void testFatal() {
        Exception ex = new RuntimeException("Fatal error");
        cbExtLogger.fatal(ex);
        verify(mockLogger).log(eq(Level.FATAL), contains("Fatal error"));
    }

    @Test
    void testTrace() {
        cbExtLogger.trace("trace msg");
        verify(mockLogger).log(Level.TRACE, "trace msg");
    }

    @Test
    void testPerformance() {
        cbExtLogger.performance("perf msg");
        verify(mockLogger).log(argThat(level -> level.name().equals("PERF")), eq("perf msg"));
    }

    @Test
    void testLoggerLevelFlags() {
        assertTrue(cbExtLogger.isDebugEnabled());
        assertTrue(cbExtLogger.isInfoEnabled());
        assertTrue(cbExtLogger.isTraceEnabled());
    }

    @Test
    void testFatal_withNullException_shouldThrowNPE() {
        NullPointerException exception = assertThrows(NullPointerException.class, () -> {
            cbExtLogger.fatal(null);
        });

        assertTrue(exception.getMessage().contains("null"));
    }
}
