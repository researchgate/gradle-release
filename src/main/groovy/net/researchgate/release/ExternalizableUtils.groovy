package net.researchgate.release

import org.gradle.api.provider.Property

class ExternalizableUtils {
    static void writeNullableStringProperty(ObjectOutput objectOutput, Property<String> stringProperty) {
        if (stringProperty.isPresent()) {
            objectOutput.writeBoolean(true)
            objectOutput.writeUTF(stringProperty.get())
        } else {
            objectOutput.writeBoolean(false)
        }
    }

    static void writeNullableBooleanProperty(ObjectOutput objectOutput, Property<Boolean> booleanProperty) {
        if (booleanProperty.isPresent()) {
            objectOutput.writeBoolean(true)
            objectOutput.writeBoolean(booleanProperty.get())
        } else {
            objectOutput.writeBoolean(false)
        }
    }

    static void readNullableStringProperty(ObjectInput objectInput, Property<String> stringProperty) throws IOException {
        if (objectInput.readBoolean()) {
            stringProperty.set(objectInput.readUTF())
        }
    }

    static void readNullableBooleanProperty(ObjectInput objectInput, Property<Boolean> booleanProperty) throws IOException {
        if (objectInput.readBoolean()) {
            booleanProperty.set(objectInput.readBoolean())
        }
    }
}
