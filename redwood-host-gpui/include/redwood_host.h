#pragma once
#include <stddef.h>
#include <stdint.h>

#if defined(_WIN32) || defined(_WIN64)
#  ifdef REDWOOD_HOST_EXPORTS
#    define RWD_API __declspec(dllexport)
#  else
#    define RWD_API __declspec(dllimport)
#  endif
#else
#  define RWD_API __attribute__((visibility("default")))
#endif

#ifdef __cplusplus
extern "C" {
#endif

typedef uint64_t rwd_view_id;
typedef uint64_t rwd_result;

enum {
  RWD_OK = 0,
  RWD_ERR = 1,
  RWD_INVALID_VIEW = 2,
  RWD_APPLY_TOO_LARGE = 3,
  RWD_APPLY_TIMEOUT = 4
};

// Init / teardown
RWD_API rwd_result redwood_host_init(void);
RWD_API void       redwood_host_shutdown(void);

// View lifecycle
RWD_API rwd_result redwood_host_create_view(rwd_view_id view_id);
RWD_API void       redwood_host_destroy_view(rwd_view_id view_id);

// Apply Redwood protocol change-list (UTF-8 JSON, no NUL required)
RWD_API rwd_result redwood_host_apply_changes(rwd_view_id view_id,
                                              const char* json_ptr,
                                              size_t json_len);

// Send an input event (UTF-8 JSON)
RWD_API rwd_result redwood_host_send_input(rwd_view_id view_id,
                                           const char* json_ptr,
                                           size_t json_len);

// Request a frame (non-zero if scheduled)
RWD_API int        redwood_host_request_frame(rwd_view_id view_id);

// Dispatch a button click for the button identified by its native handle.
// This triggers any onClick callback previously set on that widget.
RWD_API void       redwood_host_button_click(rwd_view_id view_id, rwd_handle button_handle);

// Optional: theme injection (UTF-8 JSON)
RWD_API rwd_result redwood_host_set_theme(rwd_view_id view_id,
                                          const char* json_ptr,
                                          size_t json_len);

// Optional: synchronous text measurement
// req: { "text": "...", "font": {...}, "maxWidth": N }
// resp: { "width": F, "height": F }
RWD_API rwd_result redwood_host_measure_text(rwd_view_id view_id,
                                             const char* req_json_ptr,
                                             size_t req_json_len,
                                             char* resp_json_ptr,
                                             size_t* resp_json_cap);

// GPUI vtable registration (static link model)
typedef long long rwd_handle;
typedef struct rwd_gpui_vtable_s {
  rwd_handle (*create_text)(void);
  rwd_handle (*create_button)(void);
  rwd_handle (*create_image)(void);
  rwd_handle (*create_row)(void);
  rwd_handle (*create_column)(void);
  void (*destroy)(rwd_handle);
  void (*append_child)(rwd_handle parent, rwd_handle child);
  void (*insert_child)(rwd_handle parent, int index, rwd_handle child);
  void (*remove_child)(rwd_handle parent, rwd_handle child);
  void (*set_padding)(rwd_handle, float l, float t, float r, float b);
  void (*set_size)(rwd_handle, const float* width_px_nullable, const float* height_px_nullable);
  void (*set_spacing)(rwd_handle, float gap_px);
  void (*set_align)(rwd_handle, int main_axis, int cross_axis);
  void (*set_text)(rwd_handle, const char* chars, size_t len);
  void (*set_button_text)(rwd_handle, const char* chars, size_t len);
  void (*set_button_enabled)(rwd_handle, int enabled);
  void (*set_image_url)(rwd_handle, const char* chars, size_t len);
  void (*set_image_fit)(rwd_handle, int fit);
  void (*set_image_radius)(rwd_handle, float radius_px);
} rwd_gpui_vtable;

RWD_API rwd_result redwood_host_set_gpui_vtable(const rwd_gpui_vtable* vtable);

// Optional: host-side logger
typedef void (*rwd_log_fn)(const char* line, size_t len);
RWD_API void       redwood_host_set_logger(rwd_log_fn logger);

#ifdef __cplusplus
} // extern "C"
#endif
