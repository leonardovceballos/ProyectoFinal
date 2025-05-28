package io.github.luismartinez.Entidades;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class Boss extends Enemy {

    protected int hp, maxHp;
    protected boolean incrementaHorizontal = true;
    protected FitViewport viewport;
    private static float WORLD_WIDTH;
    private static float WORLD_HEIGHT;

    public Boss(Vector2 position, Texture img, float speed, int hp, FitViewport viewport) {
        super(position, img, speed);
        this.hp = hp;
        this.maxHp = hp;
        this.viewport = viewport;
    }

    public int getHp() {
        return hp;
    }

    public void resetHp() {
        this.hp = maxHp;
    }

    public void bossHit(int damage) {
        hp -= damage;

        if(hp <= 0) {
            isAlive = false;
        }
    }

    public void update(float deltaTime){
        WORLD_HEIGHT = viewport.getWorldHeight();
        WORLD_WIDTH = viewport.getWorldWidth();

        if (position.y > WORLD_HEIGHT - sprite.getHeight()) {
            position.y -= (speed * deltaTime) * .75f;
        }

        if (position.x <= 0) {
            incrementaHorizontal = true;
        } else if (position.x > WORLD_WIDTH - sprite.getWidth()) {
            incrementaHorizontal = false;
        }

        if(incrementaHorizontal){
            position.x += speed * deltaTime;
        } else {
            position.x -= speed * deltaTime;
        }

        sprite.setPosition(position.x, position.y);
    }

}
