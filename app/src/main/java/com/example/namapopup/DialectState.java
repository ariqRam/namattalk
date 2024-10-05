package com.example.namapopup;

// Create a data class to hold the state for each dialect
class DialectState {
    String mode = "学習"; // Default mode

    public boolean isEnabled() {
        return !(this.mode == "未使用");
    }
}

