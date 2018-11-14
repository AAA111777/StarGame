package com.mygdx.game.sprite;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector2;
import com.mygdx.game.base.ActionListener;
import com.mygdx.game.base.ScaledTouchUpButton;
import com.mygdx.game.math.Rect;
import com.mygdx.game.screen.GameScreen;

public class NewGameButton extends ScaledTouchUpButton  {
    Game game;
    public NewGameButton(TextureAtlas atlas, ActionListener actionListener, Game game) {
        super(atlas.findRegion("button_new_game"), actionListener);
        this.game = game;
    }

    @Override
    public void resize(Rect worldBounds) {
        setBottom(worldBounds.getBottom());
        setRight(worldBounds.getRight());
    }

    @Override
    public boolean touchDown(Vector2 touch, int pointer) {
        if (isMe(touch)) {
            this.pointer = pointer;
            this.scale = PRESS_SCALE;
            this.isPressed = true;
            return true;
        }
        return false;
    }

    @Override
    public boolean touchUp(Vector2 touch, int pointer) {
        if (this.pointer != pointer || !isPressed) {
            return false;
        }
        if (isMe(touch)) {
            actionListener.actionPerformed(this);
        }
        isPressed = false;
        scale = 1f;
        return true;
    }
//    @Override
//    public void actionPerformed(Object src) {
//
//    }
}
