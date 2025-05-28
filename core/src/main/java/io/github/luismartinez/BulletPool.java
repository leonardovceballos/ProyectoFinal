package io.github.luismartinez;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Pool;
import io.github.luismartinez.Entidades.*;

class BulletPool extends Pool<Bullet> {
    private final Texture bulletTexture;
    private final float bulletSpeed;
    private final float bulletWidth;
    private final float bulletHeight;

    public BulletPool(Texture texture, float speed, float width, float height) {
        this.bulletTexture = texture;
        this.bulletSpeed = speed;
        this.bulletWidth = width;
        this.bulletHeight = height;
    }

    @Override
    protected Bullet newObject() {
        Bullet bullet = new Bullet(new Vector2(), bulletTexture, bulletSpeed);
        bullet.getSprite().setSize(bulletWidth, bulletHeight);
        return bullet;
    }

    @Override
    public void free(Bullet object) {
        super.free(object);
    }


}
