package io.github.luismartinez;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.Vector2;

public class PowerUp {
    private Sprite sprite;
    private PowerType type;
    private float duration; // en segundos

    public PowerUp(Vector2 position, Texture texture, PowerType type) {
        this.sprite = new Sprite(texture);
        this.sprite.setSize(32, 32);
        this.sprite.setPosition(position.x, position.y);
        this.type = type;
        this.duration = 10f;
    }

    public Sprite getSprite() {
        return sprite;
    }

    public PowerType getType() {
        return type;
    }

    public float getDuration() {
        return duration;
    }
}
