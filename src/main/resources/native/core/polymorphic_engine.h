#ifndef POLYMORPHIC_ENGINE_H
#define POLYMORPHIC_ENGINE_H

#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>

// Polymorphic code generation engine
typedef struct {
    uint8_t* code_buffer;
    size_t buffer_size;
    size_t code_size;
    uint32_t mutation_key;
    uint32_t generation;
} polymorphic_context_t;

// XOR-based instruction mutation
static inline void mutate_code_xor(uint8_t* code, size_t size, uint32_t key) {
    for (size_t i = 0; i < size; i++) {
        code[i] ^= (key >> (i % 4 * 8)) & 0xFF;
    }
}

// Simple substitution cipher for code
static inline void mutate_code_substitution(uint8_t* code, size_t size, uint32_t key) {
    uint8_t sbox[256];
    
    // Generate substitution box based on key
    for (int i = 0; i < 256; i++) {
        sbox[i] = i;
    }
    
    // Fisher-Yates shuffle with key as seed
    srand(key);
    for (int i = 255; i > 0; i--) {
        int j = rand() % (i + 1);
        uint8_t temp = sbox[i];
        sbox[i] = sbox[j];
        sbox[j] = temp;
    }
    
    // Apply substitution
    for (size_t i = 0; i < size; i++) {
        code[i] = sbox[code[i]];
    }
}

// Add junk instructions for obfuscation
static inline size_t add_junk_instructions(uint8_t* code, size_t size, size_t max_size) {
    if (size >= max_size - 10) return size;
    
    uint8_t junk_patterns[][4] = {
        {0x90, 0x90, 0x90, 0x90},  // NOP NOP NOP NOP
        {0x40, 0x48, 0x40, 0x48},  // INC EAX, DEC EAX, INC EAX, DEC EAX
        {0x50, 0x58, 0x50, 0x58},  // PUSH EAX, POP EAX, PUSH EAX, POP EAX
        {0x33, 0xC0, 0x85, 0xC0}   // XOR EAX,EAX; TEST EAX,EAX
    };
    
    srand(time(NULL));
    int pattern_idx = rand() % 4;
    int junk_size = 4;
    
    // Shift existing code
    memmove(code + junk_size, code, size);
    
    // Insert junk at beginning
    memcpy(code, junk_patterns[pattern_idx], junk_size);
    
    return size + junk_size;
}

// Generate new mutation key
static inline uint32_t generate_mutation_key() {
    srand(time(NULL));
    return (rand() << 16) | rand();
}

// Initialize polymorphic context
static inline polymorphic_context_t* init_polymorphic_context(size_t buffer_size) {
    polymorphic_context_t* ctx = malloc(sizeof(polymorphic_context_t));
    if (!ctx) return NULL;
    
    ctx->code_buffer = malloc(buffer_size);
    if (!ctx->code_buffer) {
        free(ctx);
        return NULL;
    }
    
    ctx->buffer_size = buffer_size;
    ctx->code_size = 0;
    ctx->mutation_key = generate_mutation_key();
    ctx->generation = 0;
    
    return ctx;
}

// Cleanup polymorphic context
static inline void cleanup_polymorphic_context(polymorphic_context_t* ctx) {
    if (ctx) {
        if (ctx->code_buffer) {
            // Clear sensitive data
            memset(ctx->code_buffer, 0, ctx->buffer_size);
            free(ctx->code_buffer);
        }
        memset(ctx, 0, sizeof(polymorphic_context_t));
        free(ctx);
    }
}

// Load original code into context
static inline int load_original_code(polymorphic_context_t* ctx, const uint8_t* code, size_t size) {
    if (!ctx || !code || size > ctx->buffer_size) return -1;
    
    memcpy(ctx->code_buffer, code, size);
    ctx->code_size = size;
    ctx->generation = 0;
    
    return 0;
}

// Generate new polymorphic variant
static inline int generate_variant(polymorphic_context_t* ctx) {
    if (!ctx || ctx->code_size == 0) return -1;
    
    // Create working copy
    uint8_t* work_buffer = malloc(ctx->buffer_size);
    if (!work_buffer) return -1;
    
    memcpy(work_buffer, ctx->code_buffer, ctx->code_size);
    size_t work_size = ctx->code_size;
    
    // Apply transformations based on generation
    switch (ctx->generation % 4) {
        case 0:
            // XOR mutation
            mutate_code_xor(work_buffer, work_size, ctx->mutation_key);
            break;
            
        case 1:
            // Substitution mutation
            mutate_code_substitution(work_buffer, work_size, ctx->mutation_key);
            break;
            
        case 2:
            // Add junk instructions
            work_size = add_junk_instructions(work_buffer, work_size, ctx->buffer_size);
            break;
            
        case 3:
            // Combined: junk + XOR
            work_size = add_junk_instructions(work_buffer, work_size, ctx->buffer_size);
            mutate_code_xor(work_buffer, work_size, ctx->mutation_key);
            break;
    }
    
    // Update context
    memcpy(ctx->code_buffer, work_buffer, work_size);
    ctx->code_size = work_size;
    ctx->generation++;
    
    // Generate new key for next iteration
    ctx->mutation_key = generate_mutation_key();
    
    free(work_buffer);
    return 0;
}

// Get current variant
static inline const uint8_t* get_current_variant(polymorphic_context_t* ctx, size_t* size) {
    if (!ctx || !size) return NULL;
    
    *size = ctx->code_size;
    return ctx->code_buffer;
}

// Self-modifying code execution
#ifdef _WIN32
#include <windows.h>

static inline int execute_polymorphic_code(polymorphic_context_t* ctx) {
    if (!ctx || ctx->code_size == 0) return -1;
    
    // Allocate executable memory
    void* exec_mem = VirtualAlloc(NULL, ctx->code_size, 
                                  MEM_COMMIT | MEM_RESERVE, 
                                  PAGE_EXECUTE_READWRITE);
    if (!exec_mem) return -1;
    
    // Copy code to executable memory
    memcpy(exec_mem, ctx->code_buffer, ctx->code_size);
    
    // Execute the code
    typedef int (*code_func_t)();
    code_func_t code_func = (code_func_t)exec_mem;
    
    int result = code_func();
    
    // Clean up
    VirtualFree(exec_mem, 0, MEM_RELEASE);
    
    return result;
}

#elif __linux__ || __APPLE__
#include <sys/mman.h>
#include <unistd.h>

static inline int execute_polymorphic_code(polymorphic_context_t* ctx) {
    if (!ctx || ctx->code_size == 0) return -1;
    
    // Get page size
    size_t page_size = getpagesize();
    size_t alloc_size = ((ctx->code_size + page_size - 1) / page_size) * page_size;
    
    // Allocate executable memory
    void* exec_mem = mmap(NULL, alloc_size, 
                          PROT_READ | PROT_WRITE | PROT_EXEC,
                          MAP_PRIVATE | MAP_ANONYMOUS, -1, 0);
    if (exec_mem == MAP_FAILED) return -1;
    
    // Copy code to executable memory
    memcpy(exec_mem, ctx->code_buffer, ctx->code_size);
    
    // Execute the code
    typedef int (*code_func_t)();
    code_func_t code_func = (code_func_t)exec_mem;
    
    int result = code_func();
    
    // Clean up
    munmap(exec_mem, alloc_size);
    
    return result;
}
#endif

// Dynamic function pointer table for obfuscation
typedef struct {
    void* functions[256];
    uint8_t indices[256];
    size_t count;
} function_table_t;

static inline function_table_t* create_function_table() {
    function_table_t* table = malloc(sizeof(function_table_t));
    if (!table) return NULL;
    
    memset(table, 0, sizeof(function_table_t));
    
    // Shuffle indices
    for (int i = 0; i < 256; i++) {
        table->indices[i] = i;
    }
    
    srand(time(NULL));
    for (int i = 255; i > 0; i--) {
        int j = rand() % (i + 1);
        uint8_t temp = table->indices[i];
        table->indices[i] = table->indices[j];
        table->indices[j] = temp;
    }
    
    return table;
}

static inline void add_function_to_table(function_table_t* table, void* func) {
    if (!table || table->count >= 256) return;
    
    table->functions[table->count] = func;
    table->count++;
}

static inline void* get_function_from_table(function_table_t* table, size_t index) {
    if (!table || index >= table->count) return NULL;
    
    uint8_t real_index = table->indices[index % 256];
    return table->functions[real_index % table->count];
}

static inline void destroy_function_table(function_table_t* table) {
    if (table) {
        memset(table, 0, sizeof(function_table_t));
        free(table);
    }
}

#endif // POLYMORPHIC_ENGINE_H