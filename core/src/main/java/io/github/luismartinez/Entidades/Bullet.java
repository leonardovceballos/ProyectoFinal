package io.github.luismartinez.Entidades;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Pool; // Import this

public class Bullet extends Entity implements Pool.Poolable {

    public Bullet(Vector2 position, Texture img, float speed) {
        super(position, img, speed);
    }


    public void update(float deltaTime){
        this.position.y += speed * deltaTime;
        sprite.setPosition(position.x, position.y);
    }

    @Override
    public void reset() {
        this.position.set(0, 0);
        this.speed = 0;
        this.isAlive = false;
        this.setVisible(false);

        sprite.setAlpha(1f);
        sprite.setScale(1f);
        sprite.setRotation(0);
    }
}
