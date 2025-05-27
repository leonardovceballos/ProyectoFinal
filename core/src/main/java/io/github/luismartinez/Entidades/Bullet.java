package io.github.luismartinez.Entidades;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;

public class Bullet extends Entity {
    public Bullet(Vector2 position, Texture img, float speed) {
        super(position, img, speed);
    }

    public void update(float deltaTime){
        position.y += speed * deltaTime;
        sprite.setPosition(position.x, position.y);
    }

}
