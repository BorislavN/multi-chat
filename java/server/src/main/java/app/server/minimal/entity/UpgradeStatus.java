package app.server.minimal.entity;

public record UpgradeStatus(boolean wasUpgraded, boolean threwException) {
}
