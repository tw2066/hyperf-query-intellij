<?php

namespace App {
class JcGoods extends \Hyperf\Database\Model\Model
{
    protected $table = 'users';
}

class GoodsData
{
    public function getModel(): JcGoods
    {
        return new JcGoods;
    }

    public function info(int $id)
    {
        return $this->getModel()->where('<caret>')->first();
    }
}
}
