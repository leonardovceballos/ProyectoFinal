package io.github.luismartinez.Entidades;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;

public class Boss extends Enemy {

    protected int hp, maxHp;
    protected boolean incrementaHorizontal =true;

    public Boss(Vector2 position, Texture img, float speed, int hp) {
        super(position, img, speed);
        this.hp = hp;
        this.maxHp = hp;
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
        position.y -= speed * deltaTime;

        if(position.x<=10){
            incrementaHorizontal = true;
        } else if (position.x>= Gdx.graphics.getWidth()-10) {
            incrementaHorizontal =false;
        }

        if(incrementaHorizontal){
            position.x += speed * deltaTime;
        } else {
            position.x -= speed * deltaTime;
        }

        sprite.setPosition(position.x, position.y);
    }

}
