package io.github.luismartinez.Entidades;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;

public class Enemy extends Entity {
    protected boolean canShoot = true;
    protected long lastShoot = 0;

    public Enemy(Vector2 position, Texture img, float speed) {
        super(position, img, speed);
    }

    public void update(float deltaTime){
        position.y -= speed * deltaTime;
        sprite.setPosition(position.x, position.y);
    }

    public void setLastShoot(long time){
        lastShoot = time;
        canShoot = false;
    }

    public boolean canShoot(){
        if(System.currentTimeMillis() - lastShoot > 3000){
            canShoot = true;
        }
        return canShoot;
    }
}
