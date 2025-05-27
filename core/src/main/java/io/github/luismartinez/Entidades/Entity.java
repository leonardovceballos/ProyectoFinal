package io.github.luismartinez.Entidades;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;

public abstract class Entity {
    protected Vector2 position;
    protected Sprite sprite;
    protected boolean isAlive;
    protected float speed;
    private boolean visible = true;

    public Entity(Vector2 position, Texture img, float speed){
        this.position = position;
        sprite = new Sprite(img);
        sprite.setSize(1, 1);
        this.sprite.setPosition(position.x, position.y);
        isAlive = true;
        this.speed = speed;
    }

    public void update(float deltaTime){
        sprite.setPosition(position.x, position.y);
    }

    public void draw(SpriteBatch batch){
        sprite.draw(batch);
    }

    public void setSprite(Texture img2){
        this.sprite = new Sprite(img2);
    }

    public void setIsAlive(boolean isAlive){
        this.isAlive = isAlive;
    }

    public boolean isAlive(){
        return isAlive;
    }

    public void moveLeft(float deltaTime){
        position.x -= deltaTime*speed;
    }

    public void moveRight(float deltaTime){
        position.x += deltaTime*speed;
    }

    public Vector2 getPosition(){
        return position;
    }

    public void setPosition(Vector2 position){
        this.position = position;
    }

    public Sprite getSprite(){
        return sprite;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isVisible() {
        return visible;
    }
}
