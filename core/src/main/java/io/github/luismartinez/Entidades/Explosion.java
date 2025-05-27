package io.github.luismartinez.Entidades;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class Explosion {

    public ParticleEffect effect;

    public Explosion(float x, float y) {
        effect = new ParticleEffect();
        effect.load(Gdx.files.internal("particulas/Particle Park Explosion/Particle Park Explosion.p"), Gdx.files.internal("particulas/Particle Park Explosion/"));
        startEffect(x, y);
    }

    public void draw(SpriteBatch batch, float delta) {
        effect.draw(batch, delta);
    }

    private void startEffect(float x, float y) {
        effect.setPosition(x, y);
        effect.start();
    }

    public void update(float delta) {
        effect.update(delta);
    }

    public boolean isComplete() {
        return effect.isComplete();
    }

    public void dispose() {
        effect.dispose();
    }
}
