// NERV Clock - Tauri Application
// Desktop version with transparent window

#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]

use std::env;

/// Configura las variables de entorno para Linux según el backend de display
/// Esto soluciona el problema de pantalla negra en algunas distribuciones
#[cfg(target_os = "linux")]
fn configure_linux_display() {
    // Detectar si estamos en Wayland o X11
    let session_type = env::var("XDG_SESSION_TYPE").unwrap_or_default();
    let wayland_display = env::var("WAYLAND_DISPLAY").unwrap_or_default();
    let x11_display = env::var("DISPLAY").unwrap_or_default();
    
    let is_wayland = session_type == "wayland" || !wayland_display.is_empty();
    let is_x11 = session_type == "x11" || (!x11_display.is_empty() && !is_wayland);
    
    if is_wayland {
        // Configuración para Wayland
        // WebKitGTK en Wayland puede tener problemas con transparencia
        // Forzar el uso de EGL y deshabilitar dmabuf que causa pantalla negra
        env::set_var("WEBKIT_DISABLE_DMABUF_RENDERER", "1");
        env::set_var("GDK_BACKEND", "wayland");
        
        // En algunos sistemas Wayland, necesitamos esto para que funcione la transparencia
        if env::var("WEBKIT_DISABLE_COMPOSITING_MODE").is_err() {
            env::set_var("WEBKIT_DISABLE_COMPOSITING_MODE", "1");
        }
    } else if is_x11 {
        // Configuración para X11
        env::set_var("GDK_BACKEND", "x11");
        
        // Deshabilitar el renderizador dmabuf que causa problemas en X11
        env::set_var("WEBKIT_DISABLE_DMABUF_RENDERER", "1");
    } else {
        // Fallback: intentar con configuración segura
        // Si no podemos detectar, usar configuración conservadora
        env::set_var("WEBKIT_DISABLE_DMABUF_RENDERER", "1");
        env::set_var("WEBKIT_DISABLE_COMPOSITING_MODE", "1");
    }
}

#[cfg(not(target_os = "linux"))]
fn configure_linux_display() {
    // No-op en otros sistemas operativos
}

fn main() {
    // Configurar el entorno de display antes de iniciar Tauri
    configure_linux_display();
    
    tauri::Builder::default()
        .plugin(tauri_plugin_shell::init())
        .run(tauri::generate_context!())
        .expect("error while running NERV Clock");
}
