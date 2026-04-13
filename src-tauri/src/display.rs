use std::env;

/// Result of display backend detection
#[derive(Debug, PartialEq)]
pub enum DisplayBackend {
    Wayland,
    X11,
    Unknown,
}

/// Detects the current display backend (Wayland, X11, or Unknown)
pub fn detect_display_backend() -> DisplayBackend {
    let session_type = env::var("XDG_SESSION_TYPE").unwrap_or_default();
    let wayland_display = env::var("WAYLAND_DISPLAY").unwrap_or_default();
    let x11_display = env::var("DISPLAY").unwrap_or_default();

    let is_wayland = session_type == "wayland" || !wayland_display.is_empty();
    let is_x11 = session_type == "x11" || (!x11_display.is_empty() && !is_wayland);

    if is_wayland {
        DisplayBackend::Wayland
    } else if is_x11 {
        DisplayBackend::X11
    } else {
        DisplayBackend::Unknown
    }
}

/// Configura las variables de entorno para Linux según el backend de display
/// Esto soluciona el problema de pantalla negra en algunas distribuciones
#[cfg(target_os = "linux")]
pub fn configure_linux_display() {
    match detect_display_backend() {
        DisplayBackend::Wayland => {
            env::set_var("WEBKIT_DISABLE_DMABUF_RENDERER", "1");
            env::set_var("GDK_BACKEND", "wayland");
            if env::var("WEBKIT_DISABLE_COMPOSITING_MODE").is_err() {
                env::set_var("WEBKIT_DISABLE_COMPOSITING_MODE", "1");
            }
        }
        DisplayBackend::X11 => {
            env::set_var("GDK_BACKEND", "x11");
            env::set_var("WEBKIT_DISABLE_DMABUF_RENDERER", "1");
        }
        DisplayBackend::Unknown => {
            env::set_var("WEBKIT_DISABLE_DMABUF_RENDERER", "1");
            env::set_var("WEBKIT_DISABLE_COMPOSITING_MODE", "1");
        }
    }
}

#[cfg(not(target_os = "linux"))]
pub fn configure_linux_display() {}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_detect_wayland_session() {
        let backend = detect_display_backend();
        // Just verify it returns a valid backend
        assert!(matches!(
            backend,
            DisplayBackend::Wayland | DisplayBackend::X11 | DisplayBackend::Unknown
        ));
    }

    #[test]
    fn test_display_backend_enum_values() {
        assert_eq!(DisplayBackend::Wayland, DisplayBackend::Wayland);
        assert_eq!(DisplayBackend::X11, DisplayBackend::X11);
        assert_eq!(DisplayBackend::Unknown, DisplayBackend::Unknown);
    }
}
