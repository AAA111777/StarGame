package com.mygdx.game.screen;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.mygdx.game.Star2DGame;
import com.mygdx.game.Utils.EnemiesEmmiter;
import com.mygdx.game.base.ActionListener;
import com.mygdx.game.base.Base2DScreen;
import com.mygdx.game.math.Rect;
import com.mygdx.game.math.Rnd;
import com.mygdx.game.pool.BulletPool;
import com.mygdx.game.pool.EnemyPool;
import com.mygdx.game.pool.ExplosionPool;
import com.mygdx.game.sprite.Background;
import com.mygdx.game.sprite.Bullet;
import com.mygdx.game.sprite.Enemy;
import com.mygdx.game.sprite.Explosion;
import com.mygdx.game.sprite.GameOverMessage;
import com.mygdx.game.sprite.MainShip;
import com.mygdx.game.sprite.NewGameButton;
import com.mygdx.game.sprite.Star;

import java.util.List;

public class GameScreen extends Base2DScreen implements ActionListener {

    private static final int STAR_COUNT = 64;

    private Texture bgTexture;
    private Background background;

    private TextureAtlas textureAtlas;
    private Star[] stars;

    private MainShip mainShip;

    private BulletPool bulletPool;

    private Sound laserSound;
    private Sound bulletSound;
    private Sound explosionSound;
    private Music music;

    GameOverMessage gameOver;
    NewGameButton newGameButton;

    private EnemyPool enemyPool;
    private EnemiesEmmiter enemiesEmmiter;
    private ExplosionPool explosionPool;

    Game game;

    @Override
    public void show() {
        super.show();
        laserSound = Gdx.audio.newSound(Gdx.files.internal("sounds/laser.wav"));
        bulletSound = Gdx.audio.newSound(Gdx.files.internal("sounds/bullet.wav"));
        explosionSound = Gdx.audio.newSound(Gdx.files.internal("sounds/explosion.wav"));

        bgTexture = new Texture("bg.png");
        background = new Background(new TextureRegion(bgTexture));
        textureAtlas = new TextureAtlas("mainAtlas.tpack");
        stars = new Star[STAR_COUNT];
        for (int i = 0; i < stars.length; i++) {
            stars[i] = new Star(textureAtlas);
        }
        explosionPool = new ExplosionPool(textureAtlas, explosionSound);
        bulletPool = new BulletPool();
        mainShip = new MainShip(textureAtlas, bulletPool, explosionPool, laserSound);

        enemyPool = new EnemyPool(bulletPool, explosionPool, worldBounds, bulletSound);
        enemiesEmmiter = new EnemiesEmmiter(enemyPool, worldBounds, textureAtlas);

        music = Gdx.audio.newMusic(Gdx.files.internal("sounds/music.mp3"));
        music.setLooping(true);
        music.play();
        //  инициализация картинки
        gameOver = new GameOverMessage(textureAtlas);
        newGameButton = new NewGameButton(textureAtlas, new ActionListener() {
            @Override
            public void actionPerformed(Object src) {
            }
        }, new Game() {
            @Override
            public void create() {
                game.setScreen(new GameScreen());
            }
        }
        );
    }

    @Override
    public void render(float delta) {
        super.render(delta);
        update(delta);
        checkCollisions();
        deleteAllDestroyed();
        draw();
    }

    public void update(float delta) {
        for (int i = 0; i < stars.length; i++) {
            stars[i].update(delta);
        }
        mainShip.update(delta);
        bulletPool.updateActiveObjects(delta);
        enemyPool.updateActiveObjects(delta);
        explosionPool.updateActiveObjects(delta);
        enemiesEmmiter.generate(delta);
    }

    public void checkCollisions() {
        List<Enemy> enemyList = enemyPool.getActiveObjects();
        for (Enemy enemy : enemyList) {
            if (enemy.isDestroyed()) {
                continue;
            }
            float minDist = enemy.getHalfWidth() + mainShip.getHalfWidth();
            // если столкнулись корабли
            if (enemy.pos.dst2(mainShip.pos) < minDist * minDist) {
                enemy.destroy();
                mainShip.boom();
                mainShip.destroy();
                gameOver();
                return;
            }
        }
        List<Bullet> bulletList = bulletPool.getActiveObjects();
        for (Bullet bullet : bulletList) {
            if (bullet.isDestroyed() || bullet.getOwner() == mainShip) {
                continue;
            }
            if (mainShip.isBulletCollision(bullet)) {
                bullet.destroy();
                mainShip.damage(bullet.getDamage());
                //  проверка уничтожения корабля пулями противника
                if (mainShip.getHP() <= 0) {
                    mainShip.boom();
                    mainShip.destroy();
                    gameOver();
                }
                return;
            }
        }

        for (Enemy enemy : enemyList) {
            if (enemy.isDestroyed()) {
                continue;
            }
            for (Bullet bullet : bulletList) {
                if (bullet.isDestroyed() || bullet.getOwner() != mainShip) {
                    continue;
                }
                if (enemy.isBulletCollision(bullet)) {
                    bullet.destroy();
                    enemy.damage(bullet.getDamage());
                    return;
                }
            }
        }

    }

    public void deleteAllDestroyed() {
        bulletPool.freeAllDestroyedActiveObjects();
        enemyPool.freeAllDestroyedActiveObjects();
        explosionPool.freeAllDestroyedActiveObjects();
    }

    public void draw() {
        Gdx.gl.glClearColor(0.128f, 0.53f, 0.9f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.begin();
        background.draw(batch);
        for (int i = 0; i < stars.length; i++) {
            stars[i].draw(batch);
        }
        if (!mainShip.isDestroyed()) {
            mainShip.draw(batch);
        }
        bulletPool.drawActiveObjects(batch);
        enemyPool.drawActiveObjects(batch);
        explosionPool.drawActiveObjects(batch);
        // условие завершения игры
        if(mainShip.isDestroyed()) {
            gameOver();
        gameOver.draw(batch);
        newGameButton.draw(batch);
        }
        batch.end();
    }
    // метод завершения игры
    private void gameOver(){
        enemyPool.destroyAllObjects();
        bulletPool.destroyAllObjects();
//        приходится не удалять все explosion так как mainShip не будет взрываться
//        explosionPool.destroyAllObjects();
        music.dispose();
        laserSound.dispose();
        gameOver.setHeightProportion(0.1f);
        gameOver.pos.set(0,0.1f);
        newGameButton.setHeightProportion(0.1f);
        newGameButton.pos.set(0,-0.1f);
    }

    @Override
    public void resize(Rect worldBounds) {
        background.resize(worldBounds);
        for (int i = 0; i < stars.length; i++) {
            stars[i].resize(worldBounds);
        }
        mainShip.resize(worldBounds);
    }

    @Override
    public void dispose() {
        bgTexture.dispose();
        textureAtlas.dispose();
        music.dispose();
        laserSound.dispose();
        bulletSound.dispose();
        super.dispose();
    }

    @Override
    public boolean keyDown(int keycode) {
        mainShip.keyDown(keycode);
        return super.keyDown(keycode);
    }

    @Override
    public boolean keyUp(int keycode) {
        mainShip.keyUp(keycode);
        return super.keyUp(keycode);
    }

    @Override
    public boolean touchDown(Vector2 touch, int pointer) {
        mainShip.touchDown(touch, pointer);
        return super.touchDown(touch, pointer);
    }

    @Override
    public boolean touchUp(Vector2 touch, int pointer) {
        mainShip.touchUp(touch, pointer);
        return super.touchUp(touch, pointer);
    }

    @Override
    public void actionPerformed(Object src) {
        if (src == newGameButton) {
            game.setScreen(new GameScreen());
        }
    }
}
