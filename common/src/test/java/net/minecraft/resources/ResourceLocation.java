package net.minecraft.resources;

public class ResourceLocation {
    private final String namespace;
    private final String path;

    public ResourceLocation(String namespace, String path) {
        this.namespace = namespace;
        this.path = path;
    }

    public ResourceLocation(String location) {
        String[] parts = location.split(":", 2);
        if (parts.length > 1) {
            this.namespace = parts[0];
            this.path = parts[1];
        } else {
            this.namespace = "minecraft";
            this.path = parts[0];
        }
    }

    public String getNamespace() { return namespace; }
    public String getPath() { return path; }

    @Override
    public String toString() {
        return namespace + ":" + path;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ResourceLocation other)) return false;
        return namespace.equals(other.namespace) && path.equals(other.path);
    }

    @Override
    public int hashCode() {
        return 31 * namespace.hashCode() + path.hashCode();
    }
}
