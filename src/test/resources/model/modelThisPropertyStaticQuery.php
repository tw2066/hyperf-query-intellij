<?php

namespace App {
class GoodsTagModel extends \Hyperf\Database\Model\Model
{
    protected $table = 'users';
}

class GoodsTagData
{
    /**
     * @var GoodsTagModel
     */
    public $model = GoodsTagModel::class;

    public function info(int $id)
    {
        return $this->model::query()
            ->where('<caret>')
            ->first();
    }
}
}
