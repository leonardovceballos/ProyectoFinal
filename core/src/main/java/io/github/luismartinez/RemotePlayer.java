package io.github.luismartinez;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import io.github.luismartinez.Entidades.*;

public class RemotePlayer extends Entity {
    private String id;

    public RemotePlayer(String id, Vector2 position, Texture img) {
        super(position, img, 0);
        this.id = id;
        sprite.setSize(50, 50);
    }

    public String getId() {
        return id;
    }

    public void setPosition(Vector2 newPosition) {
        this.position.set(newPosition);
        this.sprite.setPosition(newPosition.x, newPosition.y);
    }

    public void update(float delta) {
    }
}
