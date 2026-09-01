<?php

namespace App {
class JcGoods extends \Hyperf\Database\Model\Model
{
    protected $table = 'users';
}

class GoodsData
{
    /**
     * @return JcGoods
     */
    public function getModel2()
    {
        return new JcGoods;
    }

    public function info(int $id)
    {
        return $this->getModel2()->where('<caret>')->first();
    }
}
}
