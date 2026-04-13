// NERV Clock - Tauri Application
// Desktop version with transparent window

#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]

mod display;

fn main() {
    // Configurar el entorno de display antes de iniciar Tauri
    display::configure_linux_display();

    tauri::Builder::default()
        .plugin(tauri_plugin_shell::init())
        .run(tauri::generate_context!())
        .expect("error while running NERV Clock");
}
